package com.example.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细（展示对象）
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class OrderItemVO {

    /** 商品 ID（据此可以跳回商品详情） */
    private Long productId;

    /** 卖家 ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerName;

    /** 商品标题（下单时快照） */
    private String title;

    /** 商品封面（下单时快照） */
    private String coverImage;

    /** 成交单价（下单时快照） */
    private BigDecimal price;

    /** 商品当前状态：1 在售，2 已售出，3 已下架（商品已被删除时为 null） */
    private Integer productStatus;

    /** 商品是否已不存在（已删除），前端据此提示"商品已被删除" */
    private Boolean productDeleted;
}
