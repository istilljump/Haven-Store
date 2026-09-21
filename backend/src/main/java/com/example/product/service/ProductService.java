package com.example.product.service;

import com.example.common.Result;
import com.example.product.dto.ProductAddDTO;
import com.example.product.vo.ProductAddVO;

/**
 * 二手商品模块业务逻辑接口
 *
 * @author ZCode
 * @date 2026/09/21
 */
public interface ProductService {

    /**
     * 发布二手商品
     * <p>
     * 卖家用户 ID 从登录态解析（禁止前端传入），商品保存后默认为上架状态
     *
     * @param dto 发布入参（标题、描述、分类、价格、成色、交易方式、地址、经纬度）
     * @return 新发布商品的 ID
     */
    Result<ProductAddVO> addProduct(ProductAddDTO dto);
}
