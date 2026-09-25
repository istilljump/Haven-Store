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
 * 订单明细实体（对应数据库 order_item 表）
 * <p>
 * 标题、封面、价格都是下单那一刻的快照：商品之后被卖家改价、改名甚至删除，
 * 历史订单展示的仍然是当时的成交信息
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单明细 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** 卖家 ID */
    private Long sellerId;

    /** 商品标题（下单时快照） */
    private String title;

    /** 商品封面（下单时快照） */
    private String coverImage;

    /** 成交单价（下单时快照） */
    private BigDecimal price;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
