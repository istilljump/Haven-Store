package com.example.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台商品列表项
 * <p>
 * 在商品实体基础上补充「发布者用户名」与「分类名称」，
 * 避免前端为了展示两列而额外发请求
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "AdminProductVO", description = "管理后台商品列表项")
public class AdminProductVO implements Serializable {

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

    /** 封面图 URL */
    @ApiModelProperty("封面图URL")
    private String coverImage;

    /** 发布者用户 ID */
    @ApiModelProperty("发布者用户ID")
    private Long userId;

    /** 发布者用户名 */
    @ApiModelProperty("发布者用户名")
    private String username;

    /** 分类 ID */
    @ApiModelProperty("分类ID")
    private Integer categoryId;

    /** 分类名称 */
    @ApiModelProperty("分类名称")
    private String categoryName;

    /** 价格（元） */
    @ApiModelProperty("价格（元）")
    private BigDecimal price;

    /** 成色 */
    @ApiModelProperty("成色")
    private String productCondition;

    /** 交易方式：线上/线下 */
    @ApiModelProperty("交易方式")
    private String tradeType;

    /** 线下交易地址 */
    @ApiModelProperty("交易地址")
    private String address;

    /** 状态：1 在售，2 已售出，3 已下架 */
    @ApiModelProperty("状态：1在售 2已售出 3已下架")
    private Integer status;

    /** 发布时间 */
    @ApiModelProperty("发布时间")
    private LocalDateTime createTime;
}
