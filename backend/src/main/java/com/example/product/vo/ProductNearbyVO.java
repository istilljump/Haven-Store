package com.example.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 附近商品查询返回
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@Schema(description = "附近商品查询返回")
public class ProductNearbyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    @Schema("商品ID")
    private Long productId;

    /** 商品标题 */
    @Schema("商品标题")
    private String title;

    /** 商品价格 */
    @Schema("商品价格")
    private BigDecimal price;

    /** 商品成色 */
    @Schema("商品成色")
    private String productCondition;

    /** 交易方式 */
    @Schema("交易方式")
    private String tradeType;

    /** 距离（公里） */
    @Schema("距离（公里）")
    private Double distance;

    /** 经度 */
    @Schema("经度")
    private BigDecimal longitude;

    /** 纬度 */
    @Schema("纬度")
    private BigDecimal latitude;
}