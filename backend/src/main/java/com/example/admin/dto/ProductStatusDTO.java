package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 商品状态变更入参
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "ProductStatusDTO", description = "商品状态入参")
public class ProductStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标状态：1 在售，2 已售出，3 已下架（取值见 ProductStatusEnum） */
    @ApiModelProperty(value = "状态：1在售 2已售出 3已下架", required = true, example = "3")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
