package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 附近商品查询返回
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "ProductNearbyVO", description = "附近商品查询返回")
public class ProductNearbyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    @ApiModelProperty("商品ID")
    private Long productId;

    /** 商品标题 */
    @ApiModelProperty("商品标题")
    private String title;

    /** 商品价格 */
    @ApiModelProperty("商品价格")
    private BigDecimal price;

    /** 商品成色 */
    @ApiModelProperty("商品成色")
    private String productCondition;

    /** 交易方式 */
    @ApiModelProperty("交易方式")
    private String tradeType;

    /** 距离（公里） */
    @ApiModelProperty("距离（公里）")
    private Double distance;

    /** 经度 */
    @ApiModelProperty("经度")
    private BigDecimal longitude;

    /** 纬度 */
    @ApiModelProperty("纬度")
    private BigDecimal latitude;
}