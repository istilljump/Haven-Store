package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户启用/禁用入参
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "UserStatusDTO", description = "用户状态入参")
public class UserStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标状态：1 正常，0 禁用（取值见 UserStatusEnum） */
    @ApiModelProperty(value = "状态：1正常 0禁用", required = true, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
