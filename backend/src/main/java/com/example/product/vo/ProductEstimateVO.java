package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品估价返回
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "ProductEstimateVO", description = "商品估价返回")
public class ProductEstimateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 估价ID */
    @ApiModelProperty("估价ID")
    private Long estimateId;

    /** 估价价格（元） */
    @ApiModelProperty("估价价格（元）")
    private BigDecimal estimatedPrice;

    /** 价格范围（最低-最高） */
    @ApiModelProperty("价格范围（最低-最高）")
    private String priceRange;

    /** 估价置信度（0-100） */
    @ApiModelProperty("估价置信度（0-100）")
    private Integer confidence;

    /** 估价建议 */
    @ApiModelProperty("估价建议")
    private String suggestion;

    /** 估价时间 */
    @ApiModelProperty("估价时间")
    private LocalDateTime estimateTime;

    /** 依据类型：MARKET_DATA 平台成交行情 / RULE_BASELINE 规则基线（样本不足时降级） */
    @ApiModelProperty("依据类型：MARKET_DATA/RULE_BASELINE")
    private String basisType;

    /** 参与估价的同类成交样本数（行情路径才有值） */
    @ApiModelProperty("同类成交样本数")
    private Integer sampleSize;

    /** 同类成交中位价（行情路径才有值） */
    @ApiModelProperty("同类成交中位价")
    private BigDecimal marketMedian;

    /** 同类成交最低价（行情路径才有值） */
    @ApiModelProperty("同类成交最低价")
    private BigDecimal marketMin;

    /** 同类成交最高价（行情路径才有值） */
    @ApiModelProperty("同类成交最高价")
    private BigDecimal marketMax;

    /** 可解释依据列表（逐条人类可读，前端直接展示） */
    @ApiModelProperty("可解释依据列表")
    private List<String> reasons;
}