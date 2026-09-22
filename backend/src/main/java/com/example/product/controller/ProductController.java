package com.example.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.category.entity.Category;
import com.example.common.Result;
import com.example.product.dto.ProductAddDTO;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.service.ProductAIService;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductAddVO;
import com.example.product.vo.ProductDetailVO;
import com.example.product.vo.ProductEstimateVO;
import com.example.product.vo.ProductListVO;
import com.example.product.vo.ProductNearbyVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * 获取可用的商品分类
     *
     * @return 启用状态的分类列表（按排序值升序）
     */
    @ApiOperation(value = "获取商品分类", notes = "返回启用状态的分类列表，供发布商品页与筛选条件使用")
    @GetMapping("/categories")
    public Result<List<Category>> getCategories() {
        return Result.success(productService.listEnabledCategories());
    }

    /**
     * 搜索在售商品（前台商品列表页）
     *
     * @param keyword    关键词（商品标题）
     * @param categoryId 分类 ID
     * @param minPrice   价格下限
     * @param maxPrice   价格上限
     * @param sort       排序方式：latest 最新发布（默认）/ priceAsc / priceDesc
     * @param page       页码
     * @param pageSize   每页条数
     * @return 商品分页数据
     */
    @ApiOperation(value = "搜索商品", notes = "无需登录；只返回在售商品，支持关键词、分类、价格区间与排序")
    @GetMapping("/search")
    public Result<Page<ProductListVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(productService.searchProducts(keyword, categoryId, minPrice, maxPrice, sort, page, pageSize));
    }

    /**
     * 获取商品详情（前台商品详情页）
     *
     * @param productId 商品 ID
     * @return 商品详情
     */
    @ApiOperation(value = "获取商品详情", notes = "无需登录；每次调用会把该商品的浏览次数 +1")
    @GetMapping("/detail/{productId}")
    public Result<ProductDetailVO> getDetail(@PathVariable Long productId) {
        return Result.success(productService.getProductDetail(productId));
    }
}
