package com.example.market.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 校区品类行情（首页「本校行情速览」卡片数据源）
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "MarketCategoryStatVO", description = "校区品类行情")
public class MarketCategoryStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 品类 ID */
    @ApiModelProperty("品类ID")
    private Integer categoryId;

    /** 品类名称 */
    @ApiModelProperty("品类名称")
    private String categoryName;

    /** 近 90 天成交件数 */
    @ApiModelProperty("近90天成交件数")
    private Integer dealCount;

    /** 近 90 天成交中位价 */
    @ApiModelProperty("成交中位价")
    private BigDecimal medianPrice;
}
