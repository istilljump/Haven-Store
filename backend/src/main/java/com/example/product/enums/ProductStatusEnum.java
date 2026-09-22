package com.example.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品状态枚举
 * <p>
 * 与数据库 product.status 字段对应，系统内禁止使用魔法值判断状态
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {

    /** 商品上架（可在列表页展示与交易） */
    ON_SHELF(1, "上架"),

    /** 商品下架（不再对外展示） */
    OFF_SHELF(0, "下架");

    /** 状态码 */
    private final Integer code;

    /** 状态描述 */
    private final String desc;
}
