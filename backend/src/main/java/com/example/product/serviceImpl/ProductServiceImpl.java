package com.example.product.serviceImpl;

import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.constant.ProductConstant;
import com.example.product.dto.ProductAddDTO;
import com.example.product.entity.Product;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductAddVO;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


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
        // 8. 返回商品 ID
        return Result.success(ProductAddVO.builder().productId(product.getId()).build());
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
}
