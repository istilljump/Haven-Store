package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理员重置用户密码入参
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "AdminResetPasswordDTO", description = "管理员重置用户密码入参")
public class AdminResetPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新密码（重置后请转告用户，用户下次登录后可自行修改） */
    @ApiModelProperty(value = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须为6-20个字符")
    private String password;
}
