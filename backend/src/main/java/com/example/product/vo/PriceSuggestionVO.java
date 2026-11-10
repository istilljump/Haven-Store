package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 卖家侧降价建议
 * <p>
 * 滞销商品（挂牌超过后台设置的 staleDays 天）与同类成交行情对比后生成，
 * 「我的发布」列表据此提示卖家一键降价。只做建议不自动改价，定价权留在卖家手里
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Data
@ApiModel(value = "PriceSuggestionVO", description = "降价建议")
public class PriceSuggestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 建议价格（元）＝ 同类成交中位价 × 0.9，向下取整 */
    @ApiModelProperty("建议价格（元）")
    private BigDecimal suggestedPrice;

    /** 当前挂牌价（元） */
    @ApiModelProperty("当前挂牌价（元）")
    private BigDecimal currentPrice;

    /** 同类成交中位价（元） */
    @ApiModelProperty("同类成交中位价（元）")
    private BigDecimal marketMedian;

    /** 人类可读的依据说明 */
    @ApiModelProperty("依据说明")
    private String reason;
}
