package com.example.cart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车条目（展示对象）
 * <p>
 * 除了商品本身的信息，还带回「是否可结算」与不可结算的原因：
 * 商品可能在加入购物车之后被下架、被买走，甚至是用户自己发布的，
 * 这些都不该等到点了结算才报错，列表里就要说清楚
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class CartItemVO {

    /** 商品 ID */
    private Long productId;

    /** 商品标题 */
    private String title;

    /** 封面图 */
    private String coverImage;

    /** 当前售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 成色 */
    private String productCondition;

    /** 交易方式 */
    private String tradeType;

    /** 商品当前状态：1 在售，2 已售出，3 已下架（商品已被删除时为 null） */
    private Integer status;

    /** 卖家 ID */
    private Long sellerId;

    /** 卖家昵称（未设置昵称时回落为用户名） */
    private String sellerName;

    /** 加入购物车的时间 */
    private LocalDateTime createTime;

    /** 是否可结算（在售且不是自己发布的商品） */
    private Boolean available;

    /** 不可结算的原因（available 为 true 时为空） */
    private String unavailableReason;
}
