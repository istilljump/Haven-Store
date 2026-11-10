package com.example.product.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品改价入参（卖家「一键采纳降价建议」用）
 * <p>
 * 独立小 DTO 而非复用 ProductAddDTO：改价是单一字段操作，
 * 不应强迫前端把标题/图片等全量字段再提交一遍
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Data
@ApiModel(value = "ProductPriceDTO", description = "商品改价入参")
public class ProductPriceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新价格（元） */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @ApiModelProperty(value = "新价格（元）", required = true, example = "24.00")
    private BigDecimal price;
}
