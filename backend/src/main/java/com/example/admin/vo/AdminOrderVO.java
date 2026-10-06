package com.example.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台订单列表/详情项
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "AdminOrderVO", description = "管理后台订单")
public class AdminOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    @ApiModelProperty("订单ID")
    private Long id;

    /** 订单号 */
    @ApiModelProperty("订单号")
    private String orderNo;

    /** 买家用户 ID */
    @ApiModelProperty("买家用户ID")
    private Long buyerId;

    /** 买家名称（昵称优先） */
    @ApiModelProperty("买家名称")
    private String buyerName;

    /** 订单总金额 */
    @ApiModelProperty("订单总金额")
    private BigDecimal totalAmount;

    /** 商品件数 */
    @ApiModelProperty("商品件数")
    private Integer itemCount;

    /** 状态：1 待支付，2 已支付，3 已取消，4 已完成 */
    @ApiModelProperty("状态")
    private Integer status;

    /** 状态描述 */
    @ApiModelProperty("状态描述")
    private String statusDesc;

    /** 买家留言 */
    @ApiModelProperty("买家留言")
    private String remark;

    /** 支付时间 */
    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    /** 完成时间 */
    @ApiModelProperty("完成时间")
    private LocalDateTime finishTime;

    /** 取消时间 */
    @ApiModelProperty("取消时间")
    private LocalDateTime cancelTime;

    /** 下单时间 */
    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;

    /** 订单明细（仅详情接口返回） */
    @ApiModelProperty("订单明细")
    private List<Item> items = new ArrayList<>();

    /**
     * 订单明细项
     */
    @Data
    @ApiModel(value = "AdminOrderItem", description = "管理后台订单明细项")
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 商品 ID（商品可能已被删除） */
        @ApiModelProperty("商品ID")
        private Long productId;
        /** 商品标题（下单时快照） */
        @ApiModelProperty("商品标题")
        private String title;
        /** 商品封面（下单时快照） */
        @ApiModelProperty("商品封面")
        private String coverImage;
        /** 成交单价（下单时快照） */
        @ApiModelProperty("成交单价")
        private BigDecimal price;
        /** 卖家用户 ID */
        @ApiModelProperty("卖家用户ID")
        private Long sellerId;
        /** 卖家名称 */
        @ApiModelProperty("卖家名称")
        private String sellerName;
    }
}
