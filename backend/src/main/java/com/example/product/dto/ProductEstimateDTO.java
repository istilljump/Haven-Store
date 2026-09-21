package com.example.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品估价入参
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@Schema(description = "商品估价入参")
public class ProductEstimateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品标题 */
    @Schema(value = "商品标题", required = true, example = "iPhone 14 Pro 256GB 深空黑色")
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题不能超过100个字符")
    private String title;

    /** 商品描述 */
    @Schema(value = "商品描述", required = true, example = "95新，无划痕，电池健康度95%，原装配件齐全")
    @NotBlank(message = "商品描述不能为空")
    @Size(max = 500, message = "商品描述不能超过500个字符")
    private String description;

    /** 商品成色 */
    @Schema(value = "商品成色", required = true, example = "九成新")
    @NotNull(message = "商品成色不能为空")
    private String productCondition;

    /** 分类ID */
    @Schema(value = "分类ID", required = true, example = "1")
    @NotNull(message = "分类ID不能为空")
    private Integer categoryId;
}