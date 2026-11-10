package com.example.recommend.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 推荐条目
 * <p>
 * 所有推荐项强制携带 reason（后端模板生成的可解释文案），
 * 无 reason 的推荐不允许出接口——这是「可解释推荐」的硬约束
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "RecommendItemVO", description = "推荐条目")
public class RecommendItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    @ApiModelProperty("商品ID")
    private Long productId;

    /** 标题 */
    @ApiModelProperty("标题")
    private String title;

    /** 封面图 */
    @ApiModelProperty("封面图")
    private String coverImage;

    /** 价格 */
    @ApiModelProperty("价格")
    private BigDecimal price;

    /** 成色 */
    @ApiModelProperty("成色")
    private String productCondition;

    /** 品类名 */
    @ApiModelProperty("品类名")
    private String categoryName;

    /** 校区名 */
    @ApiModelProperty("校区名")
    private String campusName;

    /** 推荐原因（强制可解释文案） */
    @ApiModelProperty("推荐原因")
    private String reason;
}
