package com.example.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品订单实体（对应数据库 product_order 表）
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
@TableName("product_order")
public class ProductOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（对外展示，唯一） */
    private String orderNo;

    /** 买家 ID */
    private Long buyerId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 商品件数 */
    private Integer itemCount;

    /** 状态：1 待支付，2 已支付，3 已取消，4 已完成 */
    private Integer status;

    /** 买家留言 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入与更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
