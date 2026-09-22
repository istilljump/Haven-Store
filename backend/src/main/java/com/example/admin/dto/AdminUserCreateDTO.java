package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理后台新增用户入参
 * <p>
 * 与前台注册（RegisterDTO）的区别：管理员可以指定该账号是否为管理员，
 * 手机号允许留空（后台代建的账号可能只有用户名和密码）
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "AdminUserCreateDTO", description = "管理后台新增用户入参")
public class AdminUserCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名（登录账号，全局唯一） */
    @ApiModelProperty(value = "用户名", required = true, example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度须为3-20个字符")
    private String username;

    /** 登录密码 */
    @ApiModelProperty(value = "密码", required = true)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须为6-20个字符")
    private String password;

    /** 手机号（可选，填写时须唯一） */
    @ApiModelProperty(value = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 昵称（可选，留空时默认取用户名） */
    @ApiModelProperty(value = "昵称", example = "张三")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /** 是否创建为管理员账号（不传或 false 表示普通用户） */
    @ApiModelProperty(value = "是否管理员", example = "false")
    private Boolean isAdmin;
}
