package com.example.market.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成交明细行（批量行情统计用）
 * <p>
 * 把「品类 × 成色 × 成交价」明细一次性取回内存，由调用方分组计算
 * 中位价/价格分位，避免逐商品回查造成 N+1
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "MarketDealStat", description = "成交明细行")
public class MarketDealStat implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 品类 ID */
    @ApiModelProperty("品类ID")
    private Integer categoryId;

    /** 成色 */
    @ApiModelProperty("成色")
    private String productCondition;

    /** 成交价 */
    @ApiModelProperty("成交价")
    private BigDecimal price;

    public MarketDealStat() {
    }

    public MarketDealStat(Integer categoryId, String productCondition, BigDecimal price) {
        this.categoryId = categoryId;
        this.productCondition = productCondition;
        this.price = price;
    }
}
