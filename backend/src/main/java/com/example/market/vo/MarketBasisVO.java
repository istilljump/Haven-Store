package com.example.market.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成交行情基线（估价与划算指数共用的数据底座）
 * <p>
 * 由平台自身成交记录（order_item 快照 × 已支付/已完成订单）聚合而来，
 * 是「数据驱动定价」招牌能力的可解释依据：样本量、区间与中位价原样透出，
 * 前端与接口文档可以直接引用，不做任何加工修饰
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Data
@ApiModel(value = "MarketBasisVO", description = "成交行情基线")
public class MarketBasisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 依据类型：MARKET_DATA 有成交行情 / RULE_BASELINE 规则基线（样本不足时降级） */
    @ApiModelProperty("依据类型：MARKET_DATA/RULE_BASELINE")
    private String basisType;

    /** 统计窗口内的成交样本数 */
    @ApiModelProperty("近 N 天同类成交样本数")
    private Integer sampleSize;

    /** 统计窗口（天） */
    @ApiModelProperty("统计窗口（天）")
    private Integer days;

    /** 最低成交价 */
    @ApiModelProperty("窗口内最低成交价")
    private BigDecimal minPrice;

    /** 中位成交价（估价默认采用值） */
    @ApiModelProperty("窗口内中位成交价")
    private BigDecimal medianPrice;

    /** 最高成交价 */
    @ApiModelProperty("窗口内最高成交价")
    private BigDecimal maxPrice;
}
