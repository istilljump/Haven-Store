package com.example.product.service;

import com.example.common.Result;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.vo.ProductEstimateVO;

/**
 * 商品AI估价服务接口
 *
 * @author ZCode
 * @date 2026/09/21
 */
public interface ProductAIService {

    /**
     * 商品估价
     * <p>
     * 基于商品特征（标题、描述、成色、分类等）进行智能估价
     *
     * @param dto 估价参数
     * @return 估价结果
     */
    Result<ProductEstimateVO> estimatePrice(ProductEstimateDTO dto);
}