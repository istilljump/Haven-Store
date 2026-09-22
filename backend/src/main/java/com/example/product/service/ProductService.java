package com.example.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.category.entity.Category;
import com.example.common.Result;
import com.example.product.dto.ProductAddDTO;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.vo.ProductAddVO;
import com.example.product.vo.ProductNearbyVO;

import java.util.List;

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

    /**
     * 查询附近商品
     * <p>
     * 根据经纬度查询指定半径内的商品，按距离排序
     *
     * @param dto 查询参数（中心点经纬度、查询半径、分页参数）
     * @return 附近商品列表
     */
    Result<Page<ProductNearbyVO>> findNearbyProducts(ProductNearbyQueryDTO dto);

    /**
     * 查询可用的商品分类
     * <p>
     * 只返回启用状态的分类（管理后台禁用后的分类不再出现在选择列表中），按排序值升序
     *
     * @return 可用分类列表
     */
    List<Category> listEnabledCategories();
}
