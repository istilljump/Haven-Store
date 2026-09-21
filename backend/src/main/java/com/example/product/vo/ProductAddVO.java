package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品发布成功返回信息
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "ProductAddVO", description = "商品发布成功返回信息")
public class ProductAddVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新发布商品的 ID */
    @ApiModelProperty("商品ID")
    private Long productId;
}
