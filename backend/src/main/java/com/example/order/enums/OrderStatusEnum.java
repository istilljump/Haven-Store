package com.example.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 * <p>
 * 与数据库 product_order.status 字段对应，系统内禁止使用魔法值判断状态
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /** 待支付（下单后商品即被锁定，取消可释放） */
    PENDING_PAY(1, "待支付"),

    /** 已支付（本项目为模拟支付，仅做状态流转，不产生真实扣款） */
    PAID(2, "已支付"),

    /** 已取消（仅待支付可取消，商品回到在售） */
    CANCELLED(3, "已取消"),

    /** 已完成（买家确认收货） */
    FINISHED(4, "已完成");

    /** 状态码 */
    private final Integer code;

    /** 状态描述 */
    private final String desc;

    /**
     * 按状态码取中文描述，用于拼装提示信息
     *
     * @param code 状态码
     * @return 中文描述，未知状态返回「状态异常」
     */
    public static String descOf(Integer code) {
        for (OrderStatusEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item.getDesc();
            }
        }
        return "状态异常";
    }
}
