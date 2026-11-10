package com.example.recommend.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.campus.entity.Campus;
import com.example.campus.mapper.CampusMapper;
import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.product.entity.Product;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.example.recommend.entity.CategoryRelation;
import com.example.recommend.mapper.CategoryRelationMapper;
import com.example.recommend.mapper.RecommendStatMapper;
import com.example.recommend.service.RecommendService;
import com.example.recommend.vo.RecommendItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 场景化规则推荐实现类
 * <p>
 * 五条规则（R1~R5）全部确定性排序，行为可断言；统一排除自身、已售出/已下架、
 * 当前商品发布者的其他商品与调用方传入的排除集合；
 * 统计异常一律降级为空/null，绝不影响首页、搜索与详情主流程
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    /** 每个推荐位置的最大条数 */
    private static final int MAX_ITEMS = 4;

    /** 活跃品类扫描上限（近 90 天校区成交 TOP 品类数） */
    private static final int ACTIVE_CATEGORY_SCAN = 3;

    private final ProductMapper productMapper;

    private final CategoryMapper categoryMapper;

    private final CampusMapper campusMapper;

    private final CategoryRelationMapper categoryRelationMapper;

    private final RecommendStatMapper recommendStatMapper;

    private final SystemSettingService systemSettingService;

    @Override
    public List<RecommendItemVO> homeRecommend(Long campusId) {
        if (!recommendEnabled()) {
            return null;
        }
        try {
            List<RecommendItemVO> result = new ArrayList<>();
            Set<Long> seen = new LinkedHashSet<>();
            if (campusId != null) {
                // R1：校区近 90 天成交件数最多的品类 → 品类内在售（成交数已由 SQL 倒序）
                List<Map<String, Object>> active = recommendStatMapper.selectCampusCategoryDealCounts(
                        campusId, ACTIVE_CATEGORY_SCAN);
                for (Map<String, Object> row : active) {
                    if (result.size() >= MAX_ITEMS) {
                        break;
                    }
                    Object categoryId = row.get("categoryId");
                    if (categoryId == null) {
                        continue;
                    }
                    List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                            .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                            .eq(Product::getCampusId, campusId)
                            .eq(Product::getCategoryId, ((Number) categoryId).intValue())
                            .orderByDesc(Product::getCreateTime)
                            .orderByDesc(Product::getId)
                            .last("LIMIT " + MAX_ITEMS));
                    for (Product p : products) {
                        append(result, seen, p, campusName(campusId) + "近期成交活跃");
                    }
                }
                // R2 兜底：本校最新在售（成交活跃品类覆盖不到的补位）
                if (result.size() < MAX_ITEMS) {
                    List<Product> latest = productMapper.selectList(new LambdaQueryWrapper<Product>()
                            .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                            .eq(Product::getCampusId, campusId)
                            .orderByDesc(Product::getCreateTime)
                            .orderByDesc(Product::getId)
                            .last("LIMIT " + MAX_ITEMS));
                    for (Product p : latest) {
                        append(result, seen, p, "本校最新闲置");
                    }
                }
                return result;
            }
            // 无校区上下文：退化为「校园最新闲置」（前台同时会展示原最新流作为兜底）
            List<Product> latest = productMapper.selectList(new LambdaQueryWrapper<Product>()
                    .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                    .orderByDesc(Product::getCreateTime)
                    .orderByDesc(Product::getId)
                    .last("LIMIT " + MAX_ITEMS));
            for (Product p : latest) {
                append(result, seen, p, "校园最新闲置");
            }
            return result;
        } catch (Exception e) {
            log.warn("首页推荐计算失败，返回空：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<RecommendItemVO> complementaryRecommend(String keyword, Long campusId, List<Long> excludeIds) {
        if (!recommendEnabled() || !StringUtils.hasText(keyword)) {
            return null;
        }
        try {
            // 1. 搜索词 → 品类解析：先按在售商品命中，再按补全词库标签兜底（如 自行车→运动户外）
            String kw = keyword.trim();
            Integer categoryId = recommendStatMapper.resolveCategoryByProducts(kw);
            if (categoryId == null) {
                categoryId = recommendStatMapper.resolveCategoryBySuggestWord(kw);
            }
            if (categoryId == null) {
                // 宁可不推荐，不做同类凑数
                return new ArrayList<>();
            }
            // 2. 互补品类字典（weight 倒序）
            List<CategoryRelation> relations = categoryRelationMapper.selectList(
                    new LambdaQueryWrapper<CategoryRelation>()
                            .eq(CategoryRelation::getCategoryId, categoryId)
                            .eq(CategoryRelation::getStatus, 1)
                            .orderByDesc(CategoryRelation::getWeight)
                            .orderByAsc(CategoryRelation::getId));
            if (relations.isEmpty()) {
                return new ArrayList<>();
            }
            Set<Long> seen = new LinkedHashSet<>();
            if (excludeIds != null) {
                seen.addAll(excludeIds);
            }
            List<RecommendItemVO> result = new ArrayList<>();
            for (CategoryRelation relation : relations) {
                if (result.size() >= MAX_ITEMS) {
                    break;
                }
                LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                        .eq(Product::getCategoryId, relation.getRelatedCategoryId())
                        .orderByDesc(Product::getCreateTime)
                        .orderByDesc(Product::getId)
                        .last("LIMIT " + MAX_ITEMS);
                if (campusId != null) {
                    wrapper.eq(Product::getCampusId, campusId);
                }
                for (Product p : productMapper.selectList(wrapper)) {
                    if (result.size() >= MAX_ITEMS) {
                        break;
                    }
                    append(result, seen, p, "经常和「" + kw + "」一起交易");
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("互补推荐计算失败，返回空：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<RecommendItemVO> productRecommend(Long productId) {
        if (!recommendEnabled() || productId == null) {
            return null;
        }
        try {
            Product current = productMapper.selectById(productId);
            if (current == null || current.getCategoryId() == null) {
                return null;
            }
            List<RecommendItemVO> result = new ArrayList<>();
            Set<Long> seen = new LinkedHashSet<>();
            seen.add(current.getId());
            String condition = current.getProductCondition();
            // R4 三梯队：同校区+品类+成色 → 同校区+品类 → 同品类
            List<Object[]> tiers = new ArrayList<>();
            tiers.add(new Object[]{"同校区·" + condition + "·同类闲置", true, true, condition});
            tiers.add(new Object[]{"同校区·同类闲置", true, true, null});
            tiers.add(new Object[]{"同类闲置", false, false, null});
            for (Object[] tier : tiers) {
                if (result.size() >= MAX_ITEMS) {
                    break;
                }
                String reason = (String) tier[0];
                boolean needCampus = (Boolean) tier[1];
                boolean sameCondition = (Boolean) tier[2];
                String tierCondition = (String) tier[3];
                if (needCampus && current.getCampusId() == null) {
                    continue;
                }
                LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                        .eq(Product::getCategoryId, current.getCategoryId())
                        .ne(Product::getId, current.getId())
                        .orderByDesc(Product::getCreateTime)
                        .orderByDesc(Product::getId)
                        .last("LIMIT " + MAX_ITEMS);
                if (needCampus) {
                    wrapper.eq(Product::getCampusId, current.getCampusId());
                }
                if (sameCondition && tierCondition != null) {
                    wrapper.eq(Product::getProductCondition, tierCondition);
                }
                for (Product p : productMapper.selectList(wrapper)) {
                    if (result.size() >= MAX_ITEMS) {
                        break;
                    }
                    append(result, seen, p, reason);
                }
            }
            // R5 兜底：互补品类闲置
            if (result.size() < MAX_ITEMS) {
                List<CategoryRelation> relations = categoryRelationMapper.selectList(
                        new LambdaQueryWrapper<CategoryRelation>()
                                .eq(CategoryRelation::getCategoryId, current.getCategoryId())
                                .eq(CategoryRelation::getStatus, 1)
                                .orderByDesc(CategoryRelation::getWeight)
                                .orderByAsc(CategoryRelation::getId));
                for (CategoryRelation relation : relations) {
                    if (result.size() >= MAX_ITEMS) {
                        break;
                    }
                    for (Product p : productMapper.selectList(new LambdaQueryWrapper<Product>()
                            .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                            .eq(Product::getCategoryId, relation.getRelatedCategoryId())
                            .orderByDesc(Product::getCreateTime)
                            .orderByDesc(Product::getId)
                            .last("LIMIT " + MAX_ITEMS))) {
                        if (result.size() >= MAX_ITEMS) {
                            break;
                        }
                        append(result, seen, p, "经常和该商品一起交易");
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("详情页推荐计算失败，返回空：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 追加推荐项（去重、跳过已售出；自身商品在调用方的 wrapper 条件里已排除）
     */
    private void append(List<RecommendItemVO> result, Set<Long> seen, Product p, String reason) {
        if (p == null || p.getId() == null || seen.contains(p.getId())) {
            return;
        }
        if (!ProductStatusEnum.ON_SHELF.getCode().equals(p.getStatus())) {
            return;
        }
        seen.add(p.getId());
        RecommendItemVO vo = new RecommendItemVO();
        vo.setProductId(p.getId());
        vo.setTitle(p.getTitle());
        vo.setCoverImage(p.getCoverImage());
        vo.setPrice(p.getPrice());
        vo.setProductCondition(p.getProductCondition());
        vo.setCategoryName(categoryName(p.getCategoryId()));
        vo.setCampusName(p.getCampusId() == null ? null : campusName(p.getCampusId()));
        vo.setReason(reason);
        result.add(vo);
    }

    private String categoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private String campusName(Long campusId) {
        if (campusId == null) {
            return null;
        }
        Campus campus = campusMapper.selectById(campusId);
        return campus == null ? null : campus.getName();
    }

    /**
     * 推荐总开关（后台「交易设置」的 recommendEnabled，缺省开启）
     */
    private boolean recommendEnabled() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() == null || !Boolean.FALSE.equals(settings.getTrade().getRecommendEnabled());
    }
}
