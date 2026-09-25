package com.example.cart.service;

import com.example.cart.vo.AddFavoritesResultVO;
import com.example.cart.vo.CartItemVO;

import java.util.List;

/**
 * 购物车模块业务逻辑接口
 * <p>
 * 购物车与收藏共用 user_product_relation 表，只是 relation_type 不同，
 * 因此两者的互转就是「删一条关系 + 加一条关系」
 *
 * @author ZCode
 * @date 2026/09/25
 */
public interface CartService {

    /**
     * 加入购物车
     * <p>
     * 幂等：已在购物车中时不重复插入，也不报错
     *
     * @param productId 商品 ID
     * @return true 表示本次新加入，false 表示原本就在购物车中
     */
    Boolean addToCart(Long productId);

    /**
     * 移出购物车
     *
     * @param productId 商品 ID
     */
    void removeFromCart(Long productId);

    /**
     * 查询当前用户的购物车，按加入时间倒序
     *
     * @return 购物车条目列表
     */
    List<CartItemVO> listCart();

    /**
     * 当前用户购物车中的商品件数（头部角标）
     *
     * @return 件数
     */
    Integer countCart();

    /**
     * 把购物车里的商品移入收藏
     *
     * @param productId 商品 ID
     */
    void moveToFavorite(Long productId);

    /**
     * 把收藏里的商品批量加入购物车（保留收藏本身，只跳过不可购买的）
     *
     * @return 加入结果（成功加入件数与因不可购买被跳过的件数）
     */
    AddFavoritesResultVO addAllFavoritesToCart();
}
