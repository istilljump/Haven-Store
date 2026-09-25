package com.example.product.constant;

/**
 * 用户-商品关联类型常量
 * <p>
 * 收藏与购物车共用 user_product_relation 表，靠 relation_type 区分；
 * 两者可以一键互转，因此取值必须集中定义，避免各处写字符串字面量对不上
 *
 * @author ZCode
 * @date 2026/09/25
 */
public final class RelationTypeConstant {

    /** 收藏 */
    public static final String COLLECT = "collect";

    /** 购物车 */
    public static final String CART = "cart";

    private RelationTypeConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
