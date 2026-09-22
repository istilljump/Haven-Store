package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项（前台商品列表页展示用）
 * <p>
 * 在商品实体基础上补充「分类名称」与「发布者用户名」，
 * 避免前端为展示两列而额外发请求
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "ProductListVO", description = "商品列表项")
public class ProductListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    @ApiModelProperty("商品ID")
    private Long id;

    /** 商品标题 */
    @ApiModelProperty("商品标题")
    private String title;

    /** 商品描述 */
    @ApiModelProperty("商品描述")
    private String description;

    /** 封面图 URL（无图时为空） */
    @ApiModelProperty("封面图URL")
    private String coverImage;

    /** 价格（元） */
    @ApiModelProperty("价格（元）")
    private BigDecimal price;

    /** 原价（元），用于在列表上展示折扣 */
    @ApiModelProperty("原价（元）")
    private BigDecimal originalPrice;

    /** 成色 */
    @ApiModelProperty("成色")
    private String productCondition;

    /** 交易方式：线上/线下 */
    @ApiModelProperty("交易方式")
    private String tradeType;

    /** 状态：1 在售，2 已售出，3 已下架（列表页需要据此展示状态与操作按钮） */
    @ApiModelProperty("状态：1在售 2已售出 3已下架")
    private Integer status;

    /** 分类 ID */
    @ApiModelProperty("分类ID")
    private Integer categoryId;

    /** 分类名称 */
    @ApiModelProperty("分类名称")
    private String categoryName;

    /** 发布者用户名 */
    @ApiModelProperty("发布者用户名")
    private String username;

    /** 发布时间 */
    @ApiModelProperty("发布时间")
    private LocalDateTime createTime;
}
