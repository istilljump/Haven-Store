package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理员编辑用户资料入参
 * <p>
 * 用户名不可改（登录账号）；角色通过 isAdmin 显式传入，不传表示保持不变
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "AdminUserUpdateDTO", description = "管理员编辑用户资料入参")
public class AdminUserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 昵称（为空表示保持不变） */
    @ApiModelProperty(value = "昵称", example = "张三")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /** 手机号（为空字符串表示清空；填写时须全局唯一） */
    @ApiModelProperty(value = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 是否为管理员（不传表示保持不变） */
    @ApiModelProperty(value = "是否为管理员")
    private Boolean isAdmin;
}
