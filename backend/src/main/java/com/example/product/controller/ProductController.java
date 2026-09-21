package com.example.product.controller;

import com.example.common.Result;
import com.example.product.dto.ProductAddDTO;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductAddVO;
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
}
