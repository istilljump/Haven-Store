package com.example.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改密码入参
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "ChangePasswordDTO", description = "修改密码入参")
public class ChangePasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原密码 */
    @ApiModelProperty(value = "原密码", required = true)
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    /** 新密码 */
    @ApiModelProperty(value = "新密码", required = true)
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 20, message = "新密码长度须为6-20个字符")
    private String newPassword;

    /** 确认新密码 */
    @ApiModelProperty(value = "确认新密码", required = true)
    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;
}
