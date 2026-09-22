package com.example.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品状态枚举
 * <p>
 * 与数据库 product.status 字段对应，系统内禁止使用魔法值判断状态。
 * 三个状态与管理后台「商品管理」页的展示与操作语义保持一致
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {

    /** 商品在售（可在列表页展示与交易） */
    ON_SHELF(1, "在售"),

    /** 商品已售出（交易完成，不再展示） */
    SOLD(2, "已售出"),

    /** 商品已下架（被卖家或管理员下架） */
    OFF_SHELF(3, "已下架");

    /** 状态码 */
    private final Integer code;

    /** 状态描述 */
    private final String desc;
}
