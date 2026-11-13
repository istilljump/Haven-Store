package com.example.product.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.campus.entity.Campus;
import com.example.campus.mapper.CampusMapper;
import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.config.CacheConfig;
import com.example.market.service.MarketDataService;
import com.example.market.vo.MarketBasisVO;
import com.example.order.entity.OrderItem;
import com.example.order.entity.ProductOrder;
import com.example.order.enums.OrderStatusEnum;
import com.example.order.mapper.OrderItemMapper;
import com.example.order.mapper.ProductOrderMapper;
import com.example.product.constant.ProductConstant;
import com.example.product.constant.RelationTypeConstant;
import com.example.product.dto.CommentAddDTO;
import com.example.product.dto.ProductAddDTO;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.entity.Comment;
import com.example.product.entity.Product;
import com.example.product.entity.ProductImage;
import com.example.product.entity.UserProductRelation;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.CommentMapper;
import com.example.product.mapper.ProductImageMapper;
import com.example.product.mapper.ProductMapper;
import com.example.product.mapper.UserProductRelationMapper;
import com.example.product.service.CreditService;
import com.example.product.service.ProductAIService;
import com.example.product.service.ProductService;
import com.example.product.vo.CommentVO;
import com.example.product.vo.PriceSuggestionVO;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    /** 用户-商品关联数据访问对象（收藏） */
    private final UserProductRelationMapper userProductRelationMapper;

    /** 商品评论数据访问对象 */
    private final CommentMapper commentMapper;

    /** 商品图片数据访问对象 */
    private final ProductImageMapper productImageMapper;

    /** 订单明细数据访问对象（评论时校验「是否真的买过」） */
    private final OrderItemMapper orderItemMapper;

    /** 订单数据访问对象（评论时校验订单状态） */
    private final ProductOrderMapper productOrderMapper;

    /** 系统设置服务：价格上限、单次上传张数、AI 估价开关均从后台设置读取 */
    private final SystemSettingService systemSettingService;

    /** 交易事件通知服务（轮8：降价事件通知收藏者，按商品+标题去重） */
    private final com.example.message.service.TradeNotifyService tradeNotifyService;

    /** 成交行情服务：详情页划算指数与「我的发布」降价建议的数据来源 */
    private final MarketDataService marketDataService;

    /** 校区数据访问对象：发布/编辑时校验校区合法性，详情页补齐校区名称 */
    private final CampusMapper campusMapper;

    /** 卖家信用画像服务（轮4：可信交易轻量层，详情页展示） */
    private final CreditService creditService;

    /** 分类启用状态标识（与 category.status 字段对应：1 启用，0 禁用） */
    private static final int CATEGORY_STATUS_ENABLE = 1;

    /** 排序方式：价格升序 */
    private static final String SORT_PRICE_ASC = "priceAsc";

    /** 排序方式：价格降序 */
    private static final String SORT_PRICE_DESC = "priceDesc";

    /** 关联类型：收藏（与购物车共用一张关联表，取值统一放在 RelationTypeConstant） */
    private static final String RELATION_TYPE_COLLECT = RelationTypeConstant.COLLECT;

    /** 评论状态：正常（0 为隐藏） */
    private static final int COMMENT_STATUS_NORMAL = 1;

    /** 降价建议：滞销判定默认天数（后台「交易设置」的 trade.staleDays 可覆盖） */
    private static final int DEFAULT_STALE_DAYS = 14;

    /** 降价建议：最小行情样本量默认值（后台「交易设置」的 trade.minSampleSize 可覆盖） */
    private static final int DEFAULT_MIN_SAMPLE_SIZE = 3;

    /** 降价建议：建议价 = 同类成交中位价 × 该折扣（向下取整），低于此价视为已有竞争力 */
    private static final BigDecimal SUGGEST_FLOOR_RATIO = new BigDecimal("0.9");

    /**
     * 发布二手商品：
     * 依次执行登录态校验 → 交易方式与成色合法性校验 → 分类存在性校验 → 线下模式联动校验 →
     * 组装实体并默认上架状态写入数据库，返回自增商品 ID
     * <p>
     * 事务说明：商品主表 + 图片表 + 封面回写共多次写库，任一步失败整体回滚，
     * 避免「商品在图片不在」的半截数据
     *
     * @param dto 发布入参
     * @return 新发布商品的 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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
        // 5.5 校区合法性校验（可选字段：传入时必须是存在且启用的校区）
        validateCampus(dto);
        // 6. 线下交易联动校验：交易方式为线下时，地址、经度、纬度必须填写
        validateOfflineTrade(dto);
        // 7. 交易设置校验：价格上限与单次上传张数（来自管理后台「系统设置」）
        validateTradeSettings(dto);
        // 8. 组装商品实体并写入数据库（创建时间、更新时间由 MyBatis-Plus 自动填充）
        Product product = buildProduct(dto, userId);
        productMapper.insert(product);
        // 图片独立成表；cover_image 已由 buildProduct 取第一张，这里落库完整列表
        saveProductImages(product.getId(), dto);
        log.info("商品发布成功，商品ID：{}，卖家用户ID：{}，分类：{}，交易方式：{}",
                product.getId(), userId, category.getName(), dto.getTradeType());
        // 7. 获取AI估价建议（管理后台可关闭；可选功能，不影响发布流程）
        ProductEstimateVO estimateVO = null;
        if (isAutoEstimateEnabled()) {
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
     * 交易设置校验（发布与编辑共用）：
     * 商品价格不得超过后台设置的价格上限（0 表示不限制）；
     * 单次上传图片张数不得超过后台设置的上限（默认 9 张）
     */
    private void validateTradeSettings(ProductAddDTO dto) {
        SystemSettingDTO settings = systemSettingService.get();
        if (settings.getTrade() != null && settings.getTrade().getMaxPrice() != null
                && settings.getTrade().getMaxPrice() > 0
                && dto.getPrice() != null
                && dto.getPrice().compareTo(BigDecimal.valueOf(settings.getTrade().getMaxPrice())) > 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "商品价格不能超过平台上限 " + settings.getTrade().getMaxPrice() + " 元");
        }
        if (settings.getUpload() != null && settings.getUpload().getMaxImages() != null
                && dto.getImages() != null
                && dto.getImages().stream().filter(StringUtils::hasText).count() > settings.getUpload().getMaxImages()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "商品图片最多上传 " + settings.getUpload().getMaxImages() + " 张");
        }
    }

    /**
     * 校区合法性校验（发布与编辑共用）：
     * 校区是可选字段，未传直接放行；传了就必须是存在且启用的校区，
     * 防止前端传入不存在的 campusId 让筛选与面交点推荐指向空数据
     */
    private void validateCampus(ProductAddDTO dto) {
        if (dto.getCampusId() == null) {
            return;
        }
        Campus campus = campusMapper.selectById(dto.getCampusId());
        if (campus == null || campus.getStatus() == null || campus.getStatus() != 1) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "所选校区不存在或已停用");
        }
    }

    /**
     * AI 自动估价是否开启（管理后台「交易设置」的 autoEstimate 开关）
     */
    private boolean isAutoEstimateEnabled() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() == null
                || !Boolean.FALSE.equals(settings.getTrade().getAutoEstimate());
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
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setProductCondition(dto.getProductCondition());
        product.setTradeType(dto.getTradeType());
        product.setBrand(dto.getBrand());
        product.setModel(dto.getModel());
        product.setPurchaseTime(dto.getPurchaseTime());
        product.setFeatures(dto.getFeatures());
        product.setRemark(dto.getRemark());
        product.setContactName(dto.getContactName());
        product.setContactPhone(dto.getContactPhone());
        product.setCoverImage(dto.getCoverImage());
        product.setCampusId(dto.getCampusId());
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
     * 只返回启用状态的分类：管理后台把分类禁用后，发布页就不再展示它。
     * 结果缓存 10 分钟（管理端分类增删改会整组驱逐），避免高频下拉查询反复打库
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.CACHE_CATEGORIES, key = "'enabled'")
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
     * 只返回在售商品：已售出与已下架的不出现在前台列表里；
     * 传入 campusId 时只返回该校区商品（校园场景筛选）
     */
    @Override
    public Page<ProductListVO> searchProducts(String keyword, Integer categoryId, Long campusId, BigDecimal minPrice,
                                              BigDecimal maxPrice, String sort, String productCondition,
                                              Integer page, Integer pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode());
        if (StringUtils.hasText(keyword)) {
            // 关键词同时匹配标题/描述/品牌/型号，任一命中即返回
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getTitle, kw)
                    .or().like(Product::getDescription, kw)
                    .or().like(Product::getBrand, kw)
                    .or().like(Product::getModel, kw));
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (campusId != null) {
            wrapper.eq(Product::getCampusId, campusId);
        }
        if (StringUtils.hasText(productCondition)) {
            wrapper.eq(Product::getProductCondition, productCondition);
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
            vo.setOriginalPrice(p.getOriginalPrice());
            vo.setProductCondition(p.getProductCondition());
            vo.setTradeType(p.getTradeType());
            vo.setStatus(p.getStatus());
            vo.setCategoryId(p.getCategoryId());
            vo.setCategoryName(categoryNameMap.get(p.getCategoryId()));
            vo.setCampusId(p.getCampusId());
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
        vo.setImages(loadProductImages(product));
        vo.setPrice(product.getPrice());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(loadCategoryNameMap().get(product.getCategoryId()));
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStatus(product.getStatus());
        vo.setProductCondition(product.getProductCondition());
        vo.setTradeType(product.getTradeType());
        vo.setBrand(product.getBrand());
        vo.setModel(product.getModel());
        vo.setPurchaseTime(product.getPurchaseTime());
        vo.setFeatures(product.getFeatures());
        vo.setRemark(product.getRemark());
        vo.setContactName(product.getContactName());
        // 联系电话只给登录用户看，未登录时不返回，避免联系方式被随意爬取
        vo.setContactPhone(UserHolder.getUserId() == null ? null : product.getContactPhone());
        vo.setFavoriteCount(countFavorites(productId));
        vo.setFavorited(isFavorited(productId));
        vo.setCommentCount(countComments(productId));
        vo.setRatingAvg(avgRating(productId));
        vo.setCommented(isCommented(productId));
        vo.setAddress(product.getAddress());
        vo.setLongitude(product.getLongitude());
        vo.setLatitude(product.getLatitude());
        vo.setCampusId(product.getCampusId());
        if (product.getCampusId() != null) {
            Campus campus = campusMapper.selectById(product.getCampusId());
            vo.setCampusName(campus == null ? null : campus.getName());
        }
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

        // 成交行情与划算指数（后台「交易设置」的 dealBadge 总控；仅对在售商品计算，
        // 行情不可用时不展示且不影响详情主流程，与「行情是加分项不是依赖项」的口径一致）
        try {
            if (isDealBadgeEnabled() && ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())) {
                MarketBasisVO basis = marketDataService.getBasis(
                        product.getCategoryId(), product.getProductCondition());
                if (basis != null) {
                    vo.setMarketBasis(basis);
                    vo.setPricePosition(marketDataService.cheaperThanPercent(
                            product.getCategoryId(), product.getProductCondition(), product.getPrice()));
                }
            }
        } catch (Exception e) {
            log.warn("成交行情读取失败，详情页不展示行情与划算指数：{}", e.getMessage());
        }

        // 卖家信用画像（轮4）：统计失败不阻断详情主流程
        try {
            vo.setSellerCredit(creditService.getCredit(product.getUserId()));
        } catch (Exception e) {
            log.warn("卖家信用画像统计失败，详情页不展示信用：{}", e.getMessage());
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
     * 收藏商品
     * <p>
     * 依赖 (user_id, product_id, relation_type) 唯一索引防重，重复收藏视为成功，不报错
     */
    @Override
    public void addFavorite(Long productId) {
        Long userId = requireLoginUserId();
        if (productMapper.selectById(productId) == null) {
            throw new BusinessException("商品不存在");
        }
        if (isFavorited(productId)) {
            return;
        }
        UserProductRelation relation = new UserProductRelation();
        relation.setUserId(userId);
        relation.setProductId(productId);
        relation.setRelationType(RELATION_TYPE_COLLECT);
        userProductRelationMapper.insert(relation);
        log.info("用户收藏商品，用户ID：{}，商品ID：{}", userId, productId);
    }

    /**
     * 取消收藏商品
     */
    @Override
    public void removeFavorite(Long productId) {
        Long userId = requireLoginUserId();
        userProductRelationMapper.delete(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getUserId, userId)
                .eq(UserProductRelation::getProductId, productId)
                .eq(UserProductRelation::getRelationType, RELATION_TYPE_COLLECT));
        log.info("用户取消收藏，用户ID：{}，商品ID：{}", userId, productId);
    }

    /**
     * 查询当前用户收藏的商品
     * <p>
     * 需要保持收藏的先后顺序，无法直接交给 SQL 分页，故先按收藏时间取 ID 再内存分页
     * （单用户收藏量级很小，代价可接受）
     */
    @Override
    public Page<ProductListVO> listMyFavorites(Integer page, Integer pageSize) {
        Long userId = requireLoginUserId();
        List<Long> productIds = userProductRelationMapper.selectList(
                        new LambdaQueryWrapper<UserProductRelation>()
                                .eq(UserProductRelation::getUserId, userId)
                                .eq(UserProductRelation::getRelationType, RELATION_TYPE_COLLECT)
                                .orderByDesc(UserProductRelation::getCreateTime))
                .stream()
                .map(UserProductRelation::getProductId)
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        List<Product> ordered = productIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return toVoPage(ordered, page, pageSize);
    }

    /**
     * 查询当前用户发布的商品（我的发布）
     */
    @Override
    public Page<ProductListVO> listMyProducts(Integer status, Integer page, Integer pageSize) {
        Long userId = requireLoginUserId();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, userId)
                .orderByDesc(Product::getCreateTime)
                .orderByDesc(Product::getId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        Page<Product> productPage = productMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Page<ProductListVO> result = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        result.setRecords(toVoList(productPage.getRecords()));
        // 降价建议只出现在「我的发布」：滞销在售商品与同类成交行情对比后生成，普通搜索不受影响
        attachPriceSuggestions(result.getRecords());
        return result;
    }

    /**
     * 给「我的发布」列表附加降价建议
     * <p>
     * 规则：在售商品挂牌超过 staleDays 天（后台「交易设置」可调），且当前价高于
     * 同类成交中位价的九折时，建议降价到「中位价 × 0.9」（向下取整）。
     * 行情样本不足或开关（trade.priceSuggest）关闭时不给建议；单条计算失败不影响整页
     */
    private void attachPriceSuggestions(List<ProductListVO> records) {
        try {
            if (records == null || records.isEmpty()) {
                return;
            }
            // 进阶能力做成可选开关：后台「交易设置」的 priceSuggest 总控
            if (!isPriceSuggestEnabled()) {
                return;
            }
            int staleDays = staleDaysSetting();
            int minSample = minSampleSetting();
            LocalDateTime staleBefore = LocalDateTime.now().minusDays(staleDays);
            for (ProductListVO vo : records) {
                try {
                    // 只建议「在售 + 挂牌超过 staleDays + 分类成色齐全」的商品
                    if (vo == null || !ProductStatusEnum.ON_SHELF.getCode().equals(vo.getStatus())
                            || vo.getCreateTime() == null || vo.getCreateTime().isAfter(staleBefore)
                            || vo.getCategoryId() == null || vo.getProductCondition() == null) {
                        continue;
                    }
                    MarketBasisVO basis = marketDataService.getBasis(vo.getCategoryId(), vo.getProductCondition());
                    if (basis == null || basis.getSampleSize() == null || basis.getSampleSize() < minSample) {
                        continue;
                    }
                    BigDecimal floorPrice = basis.getMedianPrice()
                            .multiply(SUGGEST_FLOOR_RATIO)
                            .setScale(0, RoundingMode.FLOOR);
                    // 已低于行情九折的商品不再建议降价（已经比行情有竞争力）
                    if (vo.getPrice().compareTo(floorPrice) <= 0) {
                        continue;
                    }
                    PriceSuggestionVO suggestion = new PriceSuggestionVO();
                    suggestion.setSuggestedPrice(floorPrice);
                    suggestion.setCurrentPrice(vo.getPrice());
                    suggestion.setMarketMedian(basis.getMedianPrice());
                    suggestion.setReason(String.format(
                            "已挂牌超过%d天，近%d天同类成交中位价¥%s，建议降至¥%s提高成交概率",
                            staleDays, basis.getDays(),
                            basis.getMedianPrice().stripTrailingZeros().toPlainString(),
                            floorPrice.toPlainString()));
                    vo.setPriceSuggestion(suggestion);
                } catch (Exception e) {
                    log.warn("单件商品降价建议计算失败，商品ID：{}", vo == null ? null : vo.getId(), e);
                }
            }
        } catch (Exception e) {
            log.warn("降价建议批量计算失败，本页暂不展示建议：{}", e.getMessage());
        }
    }

    /**
     * 交易设置快捷读取：划算指数开关（缺省开启，兼容老库旧 config_json 缺字段）
     */
    private boolean isDealBadgeEnabled() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() == null || !Boolean.FALSE.equals(settings.getTrade().getDealBadge());
    }

    /**
     * 交易设置快捷读取：降价建议开关（缺省开启）
     */
    private boolean isPriceSuggestEnabled() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() == null || !Boolean.FALSE.equals(settings.getTrade().getPriceSuggest());
    }

    /**
     * 交易设置快捷读取：滞销判定天数（缺省 14 天）
     */
    private int staleDaysSetting() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() == null || settings.getTrade().getStaleDays() == null
                ? DEFAULT_STALE_DAYS : settings.getTrade().getStaleDays();
    }

    /**
     * 交易设置快捷读取：行情最小样本量（缺省 3）
     */
    private int minSampleSetting() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() == null || settings.getTrade().getMinSampleSize() == null
                ? DEFAULT_MIN_SAMPLE_SIZE : settings.getTrade().getMinSampleSize();
    }

    /**
     * 修改自己发布的商品
     * <p>
     * 越权校验：只能改自己发布且仍然存在的商品；
     * 状态校验：仅「在售」可编辑——已售出商品改价/改名会破坏订单快照与买家预期，
     * 已下架商品需先重新上架再编辑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long productId, ProductAddDTO dto) {
        Long userId = requireLoginUserId();
        Product exist = productMapper.selectById(productId);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能修改自己发布的商品");
        }
        if (!ProductStatusEnum.ON_SHELF.getCode().equals(exist.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    ProductStatusEnum.SOLD.getCode().equals(exist.getStatus())
                            ? "商品已售出，不能编辑"
                            : "商品已下架，请先重新上架再编辑");
        }
        if (!ProductConstant.VALID_TRADE_TYPES.contains(dto.getTradeType())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "交易方式不合法，仅支持：线上、线下");
        }
        if (!ProductConstant.VALID_CONDITIONS.contains(dto.getProductCondition())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "成色不合法，仅支持：全新、九成新、八成新、七成新及以下");
        }
        if (categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品分类不存在");
        }
        validateCampus(dto);
        validateOfflineTrade(dto);
        validateTradeSettings(dto);

        Product update = new Product();
        update.setId(productId);
        update.setTitle(dto.getTitle());
        update.setDescription(dto.getDescription());
        update.setCategoryId(dto.getCategoryId());
        update.setPrice(dto.getPrice());
        update.setOriginalPrice(dto.getOriginalPrice());
        update.setProductCondition(dto.getProductCondition());
        update.setTradeType(dto.getTradeType());
        update.setBrand(dto.getBrand());
        update.setModel(dto.getModel());
        update.setPurchaseTime(dto.getPurchaseTime());
        update.setFeatures(dto.getFeatures());
        update.setRemark(dto.getRemark());
        update.setContactName(dto.getContactName());
        update.setContactPhone(dto.getContactPhone());
        update.setCampusId(dto.getCampusId());
        // 未传封面图时保留原图，避免编辑一次就把图片弄丢
        if (StringUtils.hasText(dto.getCoverImage())) {
            update.setCoverImage(dto.getCoverImage());
        }
        if (ProductConstant.TRADE_TYPE_ONLINE.equals(dto.getTradeType())) {
            update.setAddress(null);
            update.setLongitude(null);
            update.setLatitude(null);
        } else {
            update.setAddress(dto.getAddress());
            update.setLongitude(dto.getLongitude());
            update.setLatitude(dto.getLatitude());
        }
        productMapper.updateById(update);
        // 传了图片列表就整体替换（前端提交的是当前完整列表）
        if (dto.getImages() != null) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
                    .eq(ProductImage::getProductId, productId));
            saveProductImages(productId, dto);
        }
        log.info("商品修改成功，商品ID：{}，卖家用户ID：{}", productId, userId);
    }

    /**
     * 修改自己发布的在售商品价格（「一键采纳降价建议」走此接口）
     * <p>
     * 越权校验与状态校验同 updateProduct：只能改自己的、仅「在售」可改价；
     * 价格上限与发布/编辑同口径（后台「交易设置」的 maxPrice，0 表示不限制）
     */
    @Override
    public void updateProductPrice(Long productId, BigDecimal price) {
        Long userId = requireLoginUserId();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品价格必须大于0");
        }
        Product exist = productMapper.selectById(productId);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能修改自己发布的商品");
        }
        if (!ProductStatusEnum.ON_SHELF.getCode().equals(exist.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "仅「在售」商品可以改价");
        }
        SystemSettingDTO settings = systemSettingService.get();
        if (settings.getTrade() != null && settings.getTrade().getMaxPrice() != null
                && settings.getTrade().getMaxPrice() > 0
                && price.compareTo(BigDecimal.valueOf(settings.getTrade().getMaxPrice())) > 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "商品价格不能超过平台上限 " + settings.getTrade().getMaxPrice() + " 元");
        }
        Product update = new Product();
        update.setId(productId);
        update.setPrice(price);
        productMapper.updateById(update);

        // 轮8：降价事件通知收藏者（涨价不通知；按接收人+标题+未读去重，反复调价不重复打扰）
        if (exist.getPrice() != null && price.compareTo(exist.getPrice()) < 0) {
            try {
                List<Long> collectorIds = userProductRelationMapper.selectList(
                                new LambdaQueryWrapper<UserProductRelation>()
                                        .eq(UserProductRelation::getProductId, productId)
                                        .eq(UserProductRelation::getRelationType, RelationTypeConstant.COLLECT))
                        .stream().map(UserProductRelation::getUserId)
                        .filter(java.util.Objects::nonNull)
                        .filter(uid -> !userId.equals(uid))
                        .distinct()
                        .collect(Collectors.toList());
                for (Long collectorId : collectorIds) {
                    String title = "收藏的「" + exist.getTitle() + "」降价了";
                    if (!tradeNotifyService.hasUnreadNotice(collectorId, title)) {
                        tradeNotifyService.notifyUser(collectorId, title,
                                "你收藏的「" + exist.getTitle() + "」价格从 ¥" + exist.getPrice()
                                        + " 降至 ¥" + price + "，比近期行情更划算时可以放心入手。");
                    }
                }
            } catch (Exception e) {
                // 通知属于增强信息，失败不影响改价主流程
            }
        }
        log.info("商品改价成功，商品ID：{}，新价格：{}，用户ID：{}", productId, price, userId);
    }

    /**
     * 下架自己发布的商品（软下架，不删数据）
     */
    @Override
    public void offlineProduct(Long productId) {
        Long userId = requireLoginUserId();
        Product exist = productMapper.selectById(productId);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能下架自己发布的商品");
        }
        Product update = new Product();
        update.setId(productId);
        update.setStatus(ProductStatusEnum.OFF_SHELF.getCode());
        productMapper.updateById(update);
        log.info("商品已下架，商品ID：{}", productId);
    }

    /**
     * 重新上架自己发布的商品
     * <p>
     * 仅「已下架」可重新上架：已售出的商品回到在售会与既有订单冲突，明确拒绝
     */
    @Override
    public void reshelfProduct(Long productId) {
        Long userId = requireLoginUserId();
        Product exist = productMapper.selectById(productId);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能上架自己发布的商品");
        }
        if (!ProductStatusEnum.OFF_SHELF.getCode().equals(exist.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "仅「已下架」的商品可以重新上架");
        }
        Product update = new Product();
        update.setId(productId);
        update.setStatus(ProductStatusEnum.ON_SHELF.getCode());
        productMapper.updateById(update);
        log.info("商品重新上架，商品ID：{}，用户ID：{}", productId, userId);
    }

    /**
     * 彻底删除自己发布的商品：仅「已下架」状态允许，清理全部关联数据
     */
    @Override
    public void purgeProduct(Long productId) {
        Long userId = requireLoginUserId();
        Product exist = productMapper.selectById(productId);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能删除自己发布的商品");
        }
        if (!ProductStatusEnum.OFF_SHELF.getCode().equals(exist.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "仅「已下架」的商品可以删除；如不再出售请先下架，已售出商品涉及订单凭证不可删除");
        }
        hardDeleteProduct(productId);
        log.info("用户删除商品，商品ID：{}，用户ID：{}", productId, userId);
    }

    /**
     * 彻底删除商品（物理删除，含全部关联数据）
     * <p>
     * 事务保证：商品、图片、评论、收藏/购物车关系要么一起删掉，要么都不动；
     * 订单与订单明细保存的是下单快照，不随商品删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hardDeleteProduct(Long productId) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId));
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getProductId, productId));
        // 收藏与购物车共用一张关联表，一并清理，避免残留指向已删除商品的孤儿关系
        userProductRelationMapper.delete(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getProductId, productId));
        productMapper.deleteById(productId);
    }

    /**
     * 分页查询商品评论（只展示正常状态的评论，按时间倒序）
     */
    @Override
    public Page<CommentVO> listComments(Long productId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getProductId, productId)
                .eq(Comment::getStatus, COMMENT_STATUS_NORMAL)
                .orderByDesc(Comment::getCreateTime)
                .orderByDesc(Comment::getId);
        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(page, pageSize), wrapper);

        // 批量回查评论人信息，避免逐条查询
        List<Long> userIds = commentPage.getRecords().stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        // 批量判定「已验证购买」：评论人中真实买过该商品的用户集合
        Set<Long> verifiedBuyerIds = commentPage.getRecords().isEmpty()
                ? Collections.emptySet()
                : findVerifiedBuyerIds(Collections.singletonList(productId));

        Page<CommentVO> result = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        result.setRecords(commentPage.getRecords().stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setContent(c.getContent());
            vo.setRating(c.getRating());
            vo.setUserId(c.getUserId());
            User author = userMap.get(c.getUserId());
            vo.setUsername(author == null ? "已注销用户" : author.getUsername());
            vo.setAvatar(author == null ? null : author.getAvatar());
            vo.setVerifiedBuyer(verifiedBuyerIds.contains(c.getUserId()));
            vo.setCreateTime(c.getCreateTime());
            // 轮8：回填评价标签（逗号串拆数组）与卖家回评
            vo.setTags(c.getTags() == null || c.getTags().isEmpty()
                    ? java.util.Collections.emptyList()
                    : java.util.Arrays.asList(c.getTags().split(",")));
            vo.setReply(c.getReply());
            vo.setReplyTime(c.getReplyTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * 发表商品评论
     * <p>
     * 评价资格与订单挂钩：只有真实买过该商品（存在本人订单且订单已支付/已完成）的用户可评，
     * 防止对在售商品随意刷分
     */
    @Override
    public void addComment(Long productId, CommentAddDTO dto) {
        Long userId = requireLoginUserId();
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (userId.equals(product.getUserId())) {
            throw new BusinessException("不能评价自己发布的商品");
        }
        if (isCommented(productId)) {
            throw new BusinessException("您已经评价过该商品");
        }
        if (!findVerifiedBuyerIds(Collections.singletonList(productId)).contains(userId)) {
            throw new BusinessException("仅购买过该商品的用户可以评价");
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setProductId(productId);
        comment.setContent(dto.getContent());
        comment.setRating(dto.getRating());
        // 轮8：预置正向标签（≤3 个，白名单校验后逗号落库）
        comment.setTags(com.example.product.constant.CommentTagConstant.validateAndJoin(dto.getTags()));
        comment.setStatus(COMMENT_STATUS_NORMAL);
        commentMapper.insert(comment);
        log.info("用户发表评价，用户ID：{}，商品ID：{}，评分：{}，标签：{}",
                userId, productId, dto.getRating(), comment.getTags());
    }

    /**
     * 卖家回评（轮8 业务迭代新增）：商品发布者对收到的评价回复一条说明
     * <p>
     * 校验链：评价存在且正常状态 → 当前用户是该商品发布者 → 尚未回评 → 内容非空 ≤500 字。
     * 评分公式不因回评变化——回评只承担「卖家回应」的展示职责
     */
    @Override
    public void replyComment(Long commentId, com.example.product.dto.CommentReplyDTO dto) {
        Long me = requireLoginUserId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || !Integer.valueOf(COMMENT_STATUS_NORMAL).equals(comment.getStatus())) {
            throw new BusinessException("评价不存在或已隐藏");
        }
        Product product = productMapper.selectById(comment.getProductId());
        if (product == null || !me.equals(product.getUserId())) {
            throw new BusinessException("只有商品发布者可以回评");
        }
        if (StringUtils.hasText(comment.getReply())) {
            throw new BusinessException("该评价已回复过");
        }
        String reply = dto.getReply() == null ? "" : dto.getReply().trim();
        if (!StringUtils.hasText(reply)) {
            throw new BusinessException("回评内容不能为空");
        }
        if (reply.length() > 500) {
            throw new BusinessException("回评内容最多 500 字");
        }
        Comment update = new Comment();
        update.setId(commentId);
        update.setReply(reply);
        update.setReplyTime(java.time.LocalDateTime.now());
        commentMapper.updateById(update);
        log.info("卖家回评成功，评论ID：{}，卖家：{}", commentId, me);
    }

    /**
     * 查询「买过指定商品的用户」集合（订单已支付或已完成才算有效交易）
     *
     * @param productIds 商品 ID 集合
     * @return 已成交买家的用户 ID 集合
     */
    private Set<Long> findVerifiedBuyerIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 1. 找到这些商品出现在哪些订单明细里
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getProductId, productIds)
                .select(OrderItem::getOrderId));
        if (items.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> orderIds = items.stream().map(OrderItem::getOrderId).distinct().collect(Collectors.toList());
        // 2. 命中订单中筛选「已支付/已完成」的买家
        List<ProductOrder> orders = productOrderMapper.selectList(new LambdaQueryWrapper<ProductOrder>()
                .in(ProductOrder::getId, orderIds)
                .in(ProductOrder::getStatus,
                        OrderStatusEnum.PAID.getCode(),
                        OrderStatusEnum.FINISHED.getCode())
                .select(ProductOrder::getBuyerId));
        return orders.stream().map(ProductOrder::getBuyerId).collect(Collectors.toSet());
    }

    /**
     * 保存商品图片列表（第一张同时作为封面冗余到 product.cover_image）
     */
    private void saveProductImages(Long productId, ProductAddDTO dto) {
        List<String> images = resolveImageList(dto);
        if (images.isEmpty()) {
            return;
        }
        // 封面以列表第一张为准，避免出现「列表顺序」与「封面」不一致
        Product coverUpdate = new Product();
        coverUpdate.setId(productId);
        coverUpdate.setCoverImage(images.get(0));
        productMapper.updateById(coverUpdate);

        for (int i = 0; i < images.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl(images.get(i));
            image.setSort(i);
            productImageMapper.insert(image);
        }
    }

    /**
     * 取本次提交的图片列表：优先 images，其次单张 coverImage
     */
    private List<String> resolveImageList(ProductAddDTO dto) {
        List<String> images = new ArrayList<>();
        if (dto.getImages() != null) {
            dto.getImages().stream()
                    .filter(StringUtils::hasText)
                    .forEach(images::add);
        }
        if (images.isEmpty() && StringUtils.hasText(dto.getCoverImage())) {
            images.add(dto.getCoverImage());
        }
        return images;
    }

    /**
     * 读取商品图片列表；图片表没有记录时退回 cover_image（兼容历史数据）
     */
    private List<String> loadProductImages(Product product) {
        List<ProductImage> rows = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, product.getId())
                        .orderByAsc(ProductImage::getSort)
                        .orderByAsc(ProductImage::getId));
        if (!rows.isEmpty()) {
            return rows.stream().map(ProductImage::getImageUrl).collect(Collectors.toList());
        }
        return StringUtils.hasText(product.getCoverImage())
                ? Collections.singletonList(product.getCoverImage())
                : new ArrayList<>();
    }

    /**
     * 统计商品评论数
     */
    private Long countComments(Long productId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getProductId, productId)
                .eq(Comment::getStatus, COMMENT_STATUS_NORMAL));
    }

    /**
     * 计算商品平均评分（无评论时返回 null，前端据此显示"暂无评分"）
     * <p>
     * 用 SQL AVG 聚合代替拉全量评论内存求平均
     */
    private Double avgRating(Long productId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<Comment>()
                .select("IFNULL(AVG(rating), 0) AS avg_rating")
                .eq("product_id", productId)
                .eq("status", COMMENT_STATUS_NORMAL);
        List<Map<String, Object>> rows = commentMapper.selectMaps(wrapper);
        if (rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        Object value = rows.get(0).get("avg_rating");
        if (!(value instanceof Number)) {
            return null;
        }
        double avg = ((Number) value).doubleValue();
        if (avg <= 0) {
            return null;
        }
        // 保留一位小数
        return Math.round(avg * 10) / 10.0;
    }

    /**
     * 判断当前登录用户是否已评价过该商品（未登录返回 false）
     */
    private boolean isCommented(Long productId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return false;
        }
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getProductId, productId)) > 0;
    }

    /**
     * 取当前登录用户 ID，未登录直接抛 401
     */
    private Long requireLoginUserId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 统计商品被收藏的次数
     */
    private Long countFavorites(Long productId) {
        return userProductRelationMapper.selectCount(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getProductId, productId)
                .eq(UserProductRelation::getRelationType, RELATION_TYPE_COLLECT));
    }

    /**
     * 判断当前登录用户是否已收藏该商品（未登录返回 false）
     */
    private boolean isFavorited(Long productId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return false;
        }
        return userProductRelationMapper.selectCount(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getUserId, userId)
                .eq(UserProductRelation::getProductId, productId)
                .eq(UserProductRelation::getRelationType, RELATION_TYPE_COLLECT)) > 0;
    }

    /**
     * 商品实体列表 -> 列表项 VO（补齐分类名与发布者）
     */
    private List<ProductListVO> toVoList(List<Product> products) {
        Map<Integer, String> categoryNameMap = loadCategoryNameMap();
        Map<Long, User> sellerMap = loadSellerMap(products);
        return products.stream().map(p -> {
            ProductListVO vo = new ProductListVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setDescription(p.getDescription());
            vo.setCoverImage(p.getCoverImage());
            vo.setPrice(p.getPrice());
            vo.setOriginalPrice(p.getOriginalPrice());
            vo.setProductCondition(p.getProductCondition());
            vo.setTradeType(p.getTradeType());
            vo.setStatus(p.getStatus());
            vo.setCategoryId(p.getCategoryId());
            vo.setCategoryName(categoryNameMap.get(p.getCategoryId()));
            vo.setCampusId(p.getCampusId());
            User seller = sellerMap.get(p.getUserId());
            vo.setUsername(seller == null ? null : seller.getUsername());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 内存分页（收藏列表用）
     */
    private Page<ProductListVO> toVoPage(List<Product> ordered, Integer page, Integer pageSize) {
        long total = ordered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(ordered.size(), from + pageSize);
        List<Product> slice = from >= ordered.size()
                ? Collections.emptyList()
                : new ArrayList<>(ordered.subList(from, to));
        Page<ProductListVO> result = new Page<>(page, pageSize, total);
        result.setRecords(toVoList(slice));
        return result;
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
