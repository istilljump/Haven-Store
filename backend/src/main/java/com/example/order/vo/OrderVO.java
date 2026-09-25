package com.example.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单（展示对象）
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class OrderVO {

    /** 订单 ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 买家 ID */
    private Long buyerId;

    /** 买家昵称（卖家视角需要展示"谁买走了"） */
    private String buyerName;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 商品件数 */
    private Integer itemCount;

    /** 状态：1 待支付，2 已支付，3 已取消，4 已完成 */
    private Integer status;

    /** 状态中文描述 */
    private String statusDesc;

    /** 买家留言 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 订单明细 */
    private List<OrderItemVO> items;
}
