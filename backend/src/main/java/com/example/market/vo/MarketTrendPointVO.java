package com.example.market.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 行情趋势点（按日聚合：当日成交件数 + 中位价）
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "MarketTrendPointVO", description = "行情趋势点")
public class MarketTrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成交日期（yyyy-MM-dd） */
    @ApiModelProperty("成交日期")
    private String date;

    /** 当日成交件数 */
    @ApiModelProperty("当日成交件数")
    private Integer count;

    /** 当日成交中位价 */
    @ApiModelProperty("当日成交中位价")
    private BigDecimal medianPrice;
}
