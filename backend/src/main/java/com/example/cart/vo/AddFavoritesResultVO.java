package com.example.cart.vo;

import lombok.Data;

/**
 * 「收藏批量加入购物车」的结果
 * <p>
 * 收藏里可能混着已售出、已下架或自己发布的商品，这些会被跳过，
 * 因此分别回传成功与跳过件数，前端才能给出准确提示，
 * 而不是笼统地说一句"已加入购物车"
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class AddFavoritesResultVO {

    /** 成功加入购物车的件数 */
    private Integer added;

    /** 因不可购买被跳过的件数 */
    private Integer skipped;
}
