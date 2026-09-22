package com.example.product.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.io.Serializable;

/**
 * 附近商品查询入参
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "ProductNearbyQueryDTO", description = "附近商品查询入参")
public class ProductNearbyQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 中心点经度 */
    @ApiModelProperty(value = "中心点经度", required = true, example = "116.316833")
    @NotNull(message = "中心点经度不能为空")
    @DecimalMin(value = "-180", message = "经度不能小于-180")
    @DecimalMax(value = "180", message = "经度不能大于180")
    private BigDecimal centerLongitude;

    /** 中心点纬度 */
    @ApiModelProperty(value = "中心点纬度", required = true, example = "39.981013")
    @NotNull(message = "中心点纬度不能为空")
    @DecimalMin(value = "-90", message = "纬度不能小于-90")
    @DecimalMax(value = "90", message = "纬度不能大于90")
    private BigDecimal centerLatitude;

    /** 查询半径（公里） */
    @ApiModelProperty(value = "查询半径（公里）", required = true, example = "5")
    @NotNull(message = "查询半径不能为空")
    @Min(value = 1, message = "查询半径最小为1公里")
    @Max(value = 50, message = "查询半径最大为50公里")
    private Integer radius;

    /** 分页页码，默认1 */
    @ApiModelProperty(value = "分页页码", example = "1")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /** 每页条数，默认10 */
    @ApiModelProperty(value = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 50, message = "每页条数最大为50")
    private Integer pageSize = 10;
}