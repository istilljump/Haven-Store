package com.example.product.serviceImpl;

import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
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

    /**
     * 发布二手商品：
     * 从登录态解析卖家用户 ID，组装商品实体并默认上架状态写入数据库，返回自增商品 ID
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
        // 2. 组装商品实体：卖家取登录态，状态默认上架（创建时间、更新时间由 MyBatis-Plus 自动填充）
        Product product = new Product();
        product.setUserId(userId);
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setCategoryId(dto.getCategoryId());
        product.setPrice(dto.getPrice());
        product.setProductCondition(dto.getProductCondition());
        product.setTradeType(dto.getTradeType());
        product.setAddress(dto.getAddress());
        product.setLongitude(dto.getLongitude());
        product.setLatitude(dto.getLatitude());
        product.setStatus(ProductStatusEnum.ON_SHELF.getCode());
        // 3. 写入数据库（主键自增回填到 product.id）
        productMapper.insert(product);
        log.info("商品发布成功，商品ID：{}，卖家用户ID：{}", product.getId(), userId);
        // 4. 返回商品 ID
        return Result.success(ProductAddVO.builder().productId(product.getId()).build());
    }
}
