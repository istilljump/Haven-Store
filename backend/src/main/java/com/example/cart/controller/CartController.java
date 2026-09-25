package com.example.cart.controller;

import com.example.cart.service.CartService;
import com.example.cart.vo.AddFavoritesResultVO;
import com.example.cart.vo.CartItemVO;
import com.example.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如加入购物车：POST /api/cart/1。
 * 所有接口均需登录，购物车归属取自登录态，不接受前端传入的用户 ID
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Api(tags = "购物车模块接口")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    /** 购物车模块业务逻辑对象 */
    private final CartService cartService;

    /**
     * 加入购物车
     *
     * @param productId 商品 ID
     * @return true 表示新加入，false 表示原本就在购物车中
     */
    @ApiOperation(value = "加入购物车", notes = "需要登录；重复加入不报错，返回 false 表示已在购物车中")
    @PostMapping("/{productId}")
    public Result<Boolean> addToCart(@PathVariable Long productId) {
        return Result.success(cartService.addToCart(productId));
    }

    /**
     * 移出购物车
     *
     * @param productId 商品 ID
     * @return 操作结果
     */
    @ApiOperation(value = "移出购物车", notes = "需要登录；不校验是否存在，重复移出也返回成功")
    @DeleteMapping("/{productId}")
    public Result<Void> removeFromCart(@PathVariable Long productId) {
        cartService.removeFromCart(productId);
        return Result.success();
    }

    /**
     * 获取购物车列表
     *
     * @return 购物车条目（含是否可结算与不可结算原因）
     */
    @ApiOperation(value = "获取购物车列表", notes = "需要登录；不可购买的商品会带回原因，供前端置灰展示")
    @GetMapping("/list")
    public Result<List<CartItemVO>> listCart() {
        return Result.success(cartService.listCart());
    }

    /**
     * 获取购物车商品件数
     *
     * @return 件数
     */
    @ApiOperation(value = "获取购物车件数", notes = "需要登录；用于头部购物车角标")
    @GetMapping("/count")
    public Result<Integer> countCart() {
        return Result.success(cartService.countCart());
    }

    /**
     * 把购物车中的商品移入收藏
     *
     * @param productId 商品 ID
     * @return 操作结果
     */
    @ApiOperation(value = "移入收藏", notes = "需要登录；等价于加入收藏并移出购物车")
    @PostMapping("/{productId}/move-to-favorite")
    public Result<Void> moveToFavorite(@PathVariable Long productId) {
        cartService.moveToFavorite(productId);
        return Result.success();
    }

    /**
     * 把收藏里的商品批量加入购物车
     *
     * @return 加入了多少件、跳过了多少件
     */
    @ApiOperation(value = "收藏批量加入购物车",
            notes = "需要登录；保留收藏本身，只跳过已售出/已下架/自己发布的商品")
    @PostMapping("/favorites/add-all")
    public Result<AddFavoritesResultVO> addAllFavoritesToCart() {
        return Result.success(cartService.addAllFavoritesToCart());
    }
}
