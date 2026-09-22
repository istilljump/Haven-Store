package com.example.product.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.constant.ProductConstant;
import com.example.product.dto.ProductAddDTO;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.entity.Product;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.example.product.service.ProductAIService;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductAddVO;
import com.example.product.vo.ProductDetailVO;
import com.example.product.vo.ProductEstimateVO;
import com.example.product.vo.ProductListVO;
import com.example.product.vo.ProductNearbyVO;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 二手商品模块业务逻辑实现类
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** 商品模块数据访问对象 */
    private final ProductMapper productMapper;

    /** 分类模块数据访问对象（用于校验分类 ID 合法性） */
    private final CategoryMapper categoryMapper;

    /** AI估价服务对象 */
    private final ProductAIService productAIService;

    /** 用户模块数据访问对象（用于补齐商品发布者信息） */
    private final UserMapper userMapper;

    /** 分类启用状态标识（与 category.status 字段对应：1 启用，0 禁用） */
    private static final int CATEGORY_STATUS_ENABLE = 1;

    /** 排序方式：价格升序 */
    private static final String SORT_PRICE_ASC = "priceAsc";

    /** 排序方式：价格降序 */
    private static final String SORT_PRICE_DESC = "priceDesc";

    /**
     * 发布二手商品：
     * 依次执行登录态校验 → 交易方式与成色合法性校验 → 分类存在性校验 → 线下模式联动校验 →
     * 组装实体并默认上架状态写入数据库，返回自增商品 ID
     *
     * @param dto 发布入参
     * @return 新发布商品的 ID
     */
    @Override
    public Result<ProductAddVO> addProduct(ProductAddDTO dto) {
        // 1. 从线程上下文获取当前登录用户 ID 作为卖家（由 JWT 拦截器写入，前端传入的用户 ID 一律不被信任）
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        // 2. 价格 Service 层兜底校验（DTO 层 @DecimalMin 已做第一道防线，此处防范绕过 DTO 校验的边界场景）
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品价格必须大于0");
        }
        // 3. 交易方式合法性校验（仅允许「线上」与「线下」，拒绝任意字符串）
        if (!ProductConstant.VALID_TRADE_TYPES.contains(dto.getTradeType())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "交易方式不合法，仅支持：线上、线下");
        }
        // 4. 成色合法性校验
        if (!ProductConstant.VALID_CONDITIONS.contains(dto.getProductCondition())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "成色不合法，仅支持：全新、九成新、八成新、七成新及以下");
        }
        // 5. 分类存在性校验（防止前端传入不存在的 categoryId 导致数据完整性问题）
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品分类不存在");
        }
        // 6. 线下交易联动校验：交易方式为线下时，地址、经度、纬度必须填写
        validateOfflineTrade(dto);
        // 7. 组装商品实体并写入数据库（创建时间、更新时间由 MyBatis-Plus 自动填充）
        Product product = buildProduct(dto, userId);
        productMapper.insert(product);
        log.info("商品发布成功，商品ID：{}，卖家用户ID：{}，分类：{}，交易方式：{}",
                product.getId(), userId, category.getName(), dto.getTradeType());
        // 7. 获取AI估价建议（可选，不影响发布流程）
        ProductEstimateVO estimateVO = null;
        try {
            ProductEstimateDTO estimateDTO = new ProductEstimateDTO();
            estimateDTO.setTitle(dto.getTitle());
            estimateDTO.setDescription(dto.getDescription());
            estimateDTO.setProductCondition(dto.getProductCondition());
            estimateDTO.setCategoryId(dto.getCategoryId());
            estimateVO = productAIService.estimatePrice(estimateDTO).getData();
        } catch (Exception e) {
            log.warn("获取AI估价建议失败，不影响商品发布", e);
        }

        // 8. 返回商品 ID 和估价建议
        ProductAddVO addVO = ProductAddVO.builder()
                .productId(product.getId())
                .estimatedPrice(estimateVO != null ? estimateVO.getEstimatedPrice() : null)
                .build();
        return Result.success(addVO);
    }

    /**
     * 线下交易联动校验：
     * 交易方式为「线下」时，地址、经度、纬度三项必填；交易方式为「线上」时允许为空
     *
     * @param dto 发布入参
     */
    private void validateOfflineTrade(ProductAddDTO dto) {
        if (ProductConstant.TRADE_TYPE_OFFLINE.equals(dto.getTradeType())) {
            if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "线下交易时地址不能为空");
            }
            if (dto.getLongitude() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "线下交易时经度不能为空");
            }
            if (dto.getLatitude() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "线下交易时纬度不能为空");
            }
        }
    }

    /**
     * 查询附近商品
     * <p>
     * 根据经纬度查询指定半径内的商品，按距离排序
     *
     * @param dto 查询参数
     * @return 附近商品列表
     */
    @Override
    public Result<Page<ProductNearbyVO>> findNearbyProducts(ProductNearbyQueryDTO dto) {
        // 1. 参数校验
        if (dto.getCenterLongitude() == null || dto.getCenterLatitude() == null || dto.getRadius() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "查询参数不能为空");
        }

        // 2. 参数校验 - 经纬度边界检查
        if (dto.getCenterLongitude() == null || dto.getCenterLatitude() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "经纬度不能为空");
        }
        if (dto.getCenterLongitude().compareTo(BigDecimal.valueOf(ProductConstant.LONGITUDE_MIN)) < 0 || 
            dto.getCenterLongitude().compareTo(BigDecimal.valueOf(ProductConstant.LONGITUDE_MAX)) > 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "经度必须在-180到180之间");
        }
        if (dto.getCenterLatitude().compareTo(BigDecimal.valueOf(ProductConstant.LATITUDE_MIN)) < 0 || 
            dto.getCenterLatitude().compareTo(BigDecimal.valueOf(ProductConstant.LATITUDE_MAX)) > 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "纬度必须在-90到90之间");
        }

        // 3. 计算矩形范围（提高查询性能）
        double earthRadius = 6371; // 地球半径（公里）
        double deltaLat = dto.getRadius() / earthRadius * (180 / Math.PI);
        double deltaLon = dto.getRadius() / (earthRadius * Math.cos(Math.toRadians(dto.getCenterLatitude().doubleValue()))) * (180 / Math.PI);

        BigDecimal minLongitude = dto.getCenterLongitude().subtract(BigDecimal.valueOf(deltaLon));
        BigDecimal maxLongitude = dto.getCenterLongitude().add(BigDecimal.valueOf(deltaLon));
        BigDecimal minLatitude = dto.getCenterLatitude().subtract(BigDecimal.valueOf(deltaLat));
        BigDecimal maxLatitude = dto.getCenterLatitude().add(BigDecimal.valueOf(deltaLat));

        // 3. 分页参数
        int pageNum = dto.getPageNum() != null ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        // 4. 查询附近商品
        List<ProductNearbyVO> products = productMapper.selectNearbyProducts(
                dto.getCenterLongitude(), 
                dto.getCenterLatitude(), 
                dto.getRadius(), 
                ProductStatusEnum.ON_SHELF.getCode(), 
                pageNum, 
                pageSize,
                offset,
                minLongitude, 
                maxLongitude, 
                minLatitude, 
                maxLatitude
        );

        // 5. 构建分页结果
        Page<ProductNearbyVO> page = new Page<>(pageNum, pageSize);
        page.setRecords(products);
        page.setTotal(getNearbyProductCount(dto.getCenterLongitude(), dto.getCenterLatitude(), dto.getRadius(),
                minLongitude, maxLongitude, minLatitude, maxLatitude));

        return Result.success(page);
    }

    /**
     * 获取附近商品总数
     */
    private long getNearbyProductCount(BigDecimal centerLongitude, BigDecimal centerLatitude, Integer radius,
                                       BigDecimal minLongitude, BigDecimal maxLongitude,
                                       BigDecimal minLatitude, BigDecimal maxLatitude) {
        // 与 selectNearbyProducts 使用同一套矩形范围条件，保证 total 与列表一致
        return productMapper.selectNearbyProductCount(centerLongitude, centerLatitude, radius,
                ProductStatusEnum.ON_SHELF.getCode(),
                minLongitude, maxLongitude, minLatitude, maxLatitude);
    }

    /**
     * 将发布入参 DTO 转换为商品实体
     * <p>
     * 卖家 ID 取登录态，商品状态默认上架，其余字段从 DTO 映射；
     * 线上交易时强制清空地址与经纬度（防止前端误传脏坐标导致 LBS 查询污染）
     *
     * @param dto    发布入参
     * @param userId 当前登录用户 ID（卖家）
     * @return 商品实体（尚未持久化，无主键 ID 与时间字段）
     */
    private Product buildProduct(ProductAddDTO dto, Long userId) {
        Product product = new Product();
        product.setUserId(userId);
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setCategoryId(dto.getCategoryId());
        product.setPrice(dto.getPrice());
        product.setProductCondition(dto.getProductCondition());
        product.setTradeType(dto.getTradeType());
        // 线上交易：地址与经纬度强制置空，杜绝脏坐标入库（前端可能传 0 值或空字符串）
        if (ProductConstant.TRADE_TYPE_ONLINE.equals(dto.getTradeType())) {
            product.setAddress(null);
            product.setLongitude(null);
            product.setLatitude(null);
        } else {
            product.setAddress(dto.getAddress());
            product.setLongitude(dto.getLongitude());
            product.setLatitude(dto.getLatitude());
        }
        product.setStatus(ProductStatusEnum.ON_SHELF.getCode());
        return product;
    }

    /**
     * 查询可用的商品分类
     * <p>
     * 只返回启用状态的分类：管理后台把分类禁用后，发布页就不再展示它
     */
    @Override
    public List<Category> listEnabledCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, CATEGORY_STATUS_ENABLE)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
    }

    /**
     * 搜索在售商品
     * <p>
     * 只返回在售商品：已售出与已下架的不出现在前台列表里
     */
    @Override
    public Page<ProductListVO> searchProducts(String keyword, Integer categoryId, BigDecimal minPrice,
                                              BigDecimal maxPrice, String sort, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode());
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getTitle, keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }
        // 排序：默认按发布时间倒序，其余两种按价格
        if (SORT_PRICE_ASC.equals(sort)) {
            wrapper.orderByAsc(Product::getPrice).orderByDesc(Product::getId);
        } else if (SORT_PRICE_DESC.equals(sort)) {
            wrapper.orderByDesc(Product::getPrice).orderByDesc(Product::getId);
        } else {
            wrapper.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);
        }

        Page<Product> productPage = productMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<Product> records = productPage.getRecords();

        Map<Integer, String> categoryNameMap = loadCategoryNameMap();
        Map<Long, User> sellerMap = loadSellerMap(records);

        Page<ProductListVO> result = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        result.setRecords(records.stream().map(p -> {
            ProductListVO vo = new ProductListVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setDescription(p.getDescription());
            vo.setCoverImage(p.getCoverImage());
            vo.setPrice(p.getPrice());
            vo.setProductCondition(p.getProductCondition());
            vo.setTradeType(p.getTradeType());
            vo.setCategoryId(p.getCategoryId());
            vo.setCategoryName(categoryNameMap.get(p.getCategoryId()));
            User seller = sellerMap.get(p.getUserId());
            vo.setUsername(seller == null ? null : seller.getUsername());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * 获取商品详情
     * <p>
     * 浏览次数用 SQL 自增（view_count = view_count + 1）而不是先查后写，
     * 并发访问下不会互相覆盖
     */
    @Override
    public ProductDetailVO getProductDetail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 浏览计数自增（在返回值之前执行，保证详情页展示的是累加后的次数）
        UpdateWrapper<Product> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", productId).setSql("view_count = view_count + 1");
        productMapper.update(null, updateWrapper);

        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setTitle(product.getTitle());
        vo.setDescription(product.getDescription());
        // 当前只有封面图一张，配图后自动出现在前端轮播里
        vo.setImages(StringUtils.hasText(product.getCoverImage())
                ? Collections.singletonList(product.getCoverImage())
                : new ArrayList<>());
        vo.setPrice(product.getPrice());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(loadCategoryNameMap().get(product.getCategoryId()));
        vo.setStatus(product.getStatus());
        vo.setProductCondition(product.getProductCondition());
        vo.setTradeType(product.getTradeType());
        vo.setAddress(product.getAddress());
        vo.setLongitude(product.getLongitude());
        vo.setLatitude(product.getLatitude());
        vo.setViewCount(product.getViewCount() == null ? 1 : product.getViewCount() + 1);
        vo.setCreateTime(product.getCreateTime());

        User seller = userMapper.selectById(product.getUserId());
        if (seller != null) {
            vo.setSellerId(seller.getId());
            vo.setSellerUsername(seller.getUsername());
            vo.setSellerAvatar(seller.getAvatar());
            vo.setSellerProductCount(productMapper.selectCount(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getUserId, seller.getId())
                            .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())));
        }
        return vo;
    }

    /**
     * 批量查询商品发布者信息（避免逐条回查造成 N+1）
     */
    private Map<Long, User> loadSellerMap(List<Product> products) {
        List<Long> userIds = products.stream()
                .map(Product::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    /**
     * 查询分类 ID -> 分类名称映射
     */
    private Map<Integer, String> loadCategoryNameMap() {
        List<Category> categories = categoryMapper.selectList(null);
        if (categories.isEmpty()) {
            return Collections.emptyMap();
        }
        return categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
