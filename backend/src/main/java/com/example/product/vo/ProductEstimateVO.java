package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}