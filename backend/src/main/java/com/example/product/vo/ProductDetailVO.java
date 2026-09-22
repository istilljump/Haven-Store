package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品详情（前台商品详情页展示用）
 * <p>
 * 说明：这里只暴露数据库真实存在的字段。原价、品牌、型号、购买时间、评价等
 * 前端模板曾用过的字段，因数据库中没有对应列，一律不返回——
 * 让页面按「无数据」渲染，而不是由后端编造占位内容
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "ProductDetailVO", description = "商品详情")
public class ProductDetailVO implements Serializable {

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

    /** 商品图片列表（当前仅封面图一张，无图时为空数组） */
    @ApiModelProperty("商品图片列表")
    private List<String> images = new ArrayList<>();

    /** 价格（元） */
    @ApiModelProperty("价格（元）")
    private BigDecimal price;

    /** 原价（元），用于展示折扣 */
    @ApiModelProperty("原价（元）")
    private BigDecimal originalPrice;

    /** 分类 ID */
    @ApiModelProperty("分类ID")
    private Integer categoryId;

    /** 分类名称 */
    @ApiModelProperty("分类名称")
    private String categoryName;

    /** 状态：1 在售，2 已售出，3 已下架 */
    @ApiModelProperty("状态：1在售 2已售出 3已下架")
    private Integer status;

    /** 成色 */
    @ApiModelProperty("成色")
    private String productCondition;

    /** 交易方式：线上/线下 */
    @ApiModelProperty("交易方式")
    private String tradeType;

    /** 品牌 */
    @ApiModelProperty("品牌")
    private String brand;

    /** 型号 */
    @ApiModelProperty("型号")
    private String model;

    /** 购买时间 */
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

    /** 联系电话（仅登录用户可见，未登录时为 null） */
    @ApiModelProperty("联系电话")
    private String contactPhone;

    /** 线下交易地址（线上交易时为空） */
    @ApiModelProperty("交易地址")
    private String address;

    /** 经度 */
    @ApiModelProperty("经度")
    private BigDecimal longitude;

    /** 纬度 */
    @ApiModelProperty("纬度")
    private BigDecimal latitude;

    /** 浏览次数 */
    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    /** 收藏次数（实时统计 user_product_relation） */
    @ApiModelProperty("收藏次数")
    private Long favoriteCount;

    /** 当前登录用户是否已收藏（未登录时为 false） */
    @ApiModelProperty("当前用户是否已收藏")
    private Boolean favorited;

    /** 发布时间 */
    @ApiModelProperty("发布时间")
    private LocalDateTime createTime;

    /** 卖家用户 ID */
    @ApiModelProperty("卖家用户ID")
    private Long sellerId;

    /** 卖家用户名 */
    @ApiModelProperty("卖家用户名")
    private String sellerUsername;

    /** 卖家头像 */
    @ApiModelProperty("卖家头像")
    private String sellerAvatar;

    /** 卖家在售商品数 */
    @ApiModelProperty("卖家在售商品数")
    private Long sellerProductCount;
}
