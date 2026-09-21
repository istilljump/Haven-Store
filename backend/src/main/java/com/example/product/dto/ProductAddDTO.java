package com.example.product.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 二手商品发布请求入参
 * <p>
 * 卖家用户 ID 不在本入参中：由服务端从登录 Token 解析，前端传入的任何用户 ID 一律不被信任
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "ProductAddDTO", description = "二手商品发布入参")
public class ProductAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品标题 */
    @ApiModelProperty(value = "商品标题", required = true, example = "95新 iPhone 13 白色 128G")
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题长度不能超过100个字符")
    private String title;

    /** 商品描述 */
    @ApiModelProperty(value = "商品描述", example = "自用一年，无拆修，成色良好，可小刀")
    @Size(max = 1000, message = "商品描述长度不能超过1000个字符")
    private String description;

    /** 分类 ID */
    @ApiModelProperty(value = "分类ID", required = true, example = "1")
    @NotNull(message = "分类ID不能为空")
    private Integer categoryId;

    /** 商品价格（元） */
    @ApiModelProperty(value = "商品价格（元）", required = true, example = "2999.00")
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "商品价格整数部分最多8位，小数最多2位")
    private BigDecimal price;

    /** 成色：全新 / 九成新 / 八成新 / 七成新及以下 */
    @ApiModelProperty(value = "成色", required = true, example = "九成新")
    @NotBlank(message = "成色不能为空")
    @Size(max = 10, message = "成色长度不能超过10个字符")
    private String productCondition;

    /** 交易方式：线上 / 线下 */
    @ApiModelProperty(value = "交易方式：线上/线下", required = true, example = "线下")
    @NotBlank(message = "交易方式不能为空")
    @Size(max = 10, message = "交易方式长度不能超过10个字符")
    private String tradeType;

    /** 线下交易地址 */
    @ApiModelProperty(value = "线下交易地址", example = "北京市海淀区中关村大街1号")
    @Size(max = 200, message = "交易地址长度不能超过200个字符")
    private String address;

    /** 经度（线下交易地点坐标） */
    @ApiModelProperty(value = "经度", example = "116.316833")
    private BigDecimal longitude;

    /** 纬度（线下交易地点坐标） */
    @ApiModelProperty(value = "纬度", example = "39.981013")
    private BigDecimal latitude;
}
