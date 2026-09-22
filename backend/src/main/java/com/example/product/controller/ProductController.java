package com.example.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.product.dto.ProductAddDTO;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.service.ProductAIService;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductAddVO;
import com.example.product.vo.ProductEstimateVO;
import com.example.product.vo.ProductNearbyVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 二手商品模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如发布接口：POST /api/product/add；
 * 模块无放行配置，全部接口需登录后携带 Token 访问
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Api(tags = "商品模块接口")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    /** 商品模块业务逻辑对象 */
    private final ProductService productService;

    /** 商品AI估价业务逻辑对象 */
    private final ProductAIService productAIService;

    /**
     * 发布二手商品
     *
     * @param dto 发布入参（标题、描述、分类ID、价格、成色、交易方式、地址、经度、纬度）
     * @return 新发布商品的 ID
     */
    @ApiOperation(value = "发布二手商品", notes = "需要登录态；卖家ID从登录 Token 解析，前端无需也无法传入；商品默认上架")
    @PostMapping("/add")
    public Result<ProductAddVO> add(@RequestBody @Validated ProductAddDTO dto) {
        return productService.addProduct(dto);
    }

    /**
     * 查询附近商品
     *
     * @param dto 查询参数（中心点经纬度、查询半径、分页参数）
     * @return 附近商品列表
     */
    @ApiOperation(value = "查询附近商品", notes = "需要登录态；根据经纬度查询指定半径内的商品，按距离排序")
    @PostMapping("/nearby")
    public Result<Page<ProductNearbyVO>> findNearbyProducts(@RequestBody @Validated ProductNearbyQueryDTO dto) {
        return productService.findNearbyProducts(dto);
    }

    /**
     * 获取AI估价建议
     *
     * @param dto 估价参数（标题、描述、成色、分类）
     * @return 估价结果
     */
    @ApiOperation(value = "获取AI估价建议", notes = "基于商品特征进行智能估价，返回建议价格和置信度")
    @PostMapping("/estimate")
    public Result<ProductEstimateVO> getAIPriceSuggestion(@RequestBody @Validated ProductEstimateDTO dto) {
        return productAIService.estimatePrice(dto);
    }
}
