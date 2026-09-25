package com.example.order.constant;

/**
 * 订单模块常量
 *
 * @author ZCode
 * @date 2026/09/25
 */
public final class OrderConstant {

    /** 订单号前缀 */
    public static final String ORDER_NO_PREFIX = "HM";

    /** 买家留言最大长度（与 product_order.remark 列宽保持一致） */
    public static final int REMARK_MAX_LENGTH = 255;

    /**
     * 单笔订单最多结算的商品件数
     * <p>
     * 二手商品基本都是单件，这里设上限只是防止一次性提交过多商品导致订单难以处理
     */
    public static final int MAX_ITEM_COUNT = 20;

    private OrderConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
