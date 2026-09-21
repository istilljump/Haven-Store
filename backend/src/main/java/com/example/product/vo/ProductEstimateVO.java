package com.example.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品估价返回
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@Schema(description = "商品估价返回")
public class ProductEstimateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 估价ID */
    @Schema("估价ID")
    private Long estimateId;

    /** 估价价格（元） */
    @Schema("估价价格（元）")
    private BigDecimal estimatedPrice;

    /** 价格范围（最低-最高） */
    @Schema("价格范围（最低-最高）")
    private String priceRange;

    /** 估价置信度（0-100） */
    @Schema("估价置信度（0-100）")
    private Integer confidence;

    /** 估价建议 */
    @Schema("估价建议")
    private String suggestion;

    /** 估价时间 */
    @Schema("估价时间")
    private LocalDateTime estimateTime;
}