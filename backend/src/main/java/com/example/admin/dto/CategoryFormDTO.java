package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 商品分类新增/修改入参
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "CategoryFormDTO", description = "分类表单入参")
public class CategoryFormDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类名称（全局唯一） */
    @ApiModelProperty(value = "分类名称", required = true, example = "手机数码")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称长度不能超过30个字符")
    private String name;

    /** 排序值（数字越小越靠前） */
    @ApiModelProperty(value = "排序值（越小越靠前）", example = "1")
    @Min(value = 1, message = "排序值不能小于1")
    @Max(value = 99, message = "排序值不能大于99")
    private Integer sort;

    /** 状态：1 启用，0 禁用 */
    @ApiModelProperty(value = "状态：1启用 0禁用", example = "1")
    private Integer status;
}
