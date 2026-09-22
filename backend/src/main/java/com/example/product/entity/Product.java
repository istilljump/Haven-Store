package com.example.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品实体类（对应数据库 product 表）
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "Product", description = "二手商品实体")
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 ID（主键，数据库自增） */
    @ApiModelProperty("商品ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布用户 ID（卖家，关联 user 表，服务端从登录态解析，禁止前端传入） */
    @ApiModelProperty("发布用户ID（卖家）")
    private Long userId;

    /** 商品标题 */
    @ApiModelProperty("商品标题")
    private String title;

    /** 商品描述 */
    @ApiModelProperty("商品描述")
    private String description;

    /** 封面图 URL（列表页缩略图展示） */
    @ApiModelProperty("封面图URL")
    private String coverImage;

    /** 分类 ID（关联 category 表） */
    @ApiModelProperty("分类ID")
    private Integer categoryId;

    /** 商品价格（元，必须大于 0） */
    @ApiModelProperty("商品价格（元）")
    private BigDecimal price;

    /** 原价（元），用于展示折扣；可为空 */
    @ApiModelProperty("原价（元）")
    private BigDecimal originalPrice;

    /** 成色：全新 / 九成新 / 八成新 / 七成新及以下 */
    @ApiModelProperty("成色：全新/九成新/八成新/七成新及以下")
    private String productCondition;

    /** 交易方式：线上 / 线下 */
    @ApiModelProperty("交易方式：线上/线下")
    private String tradeType;

    /** 品牌 */
    @ApiModelProperty("品牌")
    private String brand;

    /** 型号 */
    @ApiModelProperty("型号")
    private String model;

    /** 购买时间（发布页日期选择器的值） */
    @ApiModelProperty("购买时间")
    private String purchaseTime;

    /** 商品特色（多个用逗号分隔） */
    @ApiModelProperty("商品特色")
    private String features;

    /** 备注说明 */
    @ApiModelProperty("备注说明")
    private String remark;

    /** 联系人 */
    @ApiModelProperty("联系人")
    private String contactName;

    /** 联系电话 */
    @ApiModelProperty("联系电话")
    private String contactPhone;

    /** 线下交易地址（交易方式为线上时为空） */
    @ApiModelProperty("线下交易地址")
    private String address;

    /** 经度（线下交易地点坐标，-180 ~ 180；线上交易时为空） */
    @ApiModelProperty("经度")
    private BigDecimal longitude;

    /** 纬度（线下交易地点坐标，-90 ~ 90；线上交易时为空） */
    @ApiModelProperty("纬度")
    private BigDecimal latitude;

    /** 创建时间（插入时由 MetaObjectHandler 自动填充） */
    @ApiModelProperty("创建时间（发布时间）")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入与更新时由 MetaObjectHandler 自动填充） */
    @ApiModelProperty("更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 商品状态：1 在售，2 已售出，3 已下架（取值见 ProductStatusEnum） */
    @ApiModelProperty("商品状态：1在售 2已售出 3已下架")
    private Integer status;

    /** 浏览次数（详情页每次访问累加） */
    @ApiModelProperty("浏览次数")
    private Integer viewCount;
}
