package com.example.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户注册请求入参
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "RegisterDTO", description = "用户注册入参")
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名（登录账号） */
    @ApiModelProperty(value = "用户名", required = true, example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度须为3-20个字符")
    private String username;

    /** 登录密码 */
    @ApiModelProperty(value = "密码", required = true)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须为6-20个字符")
    private String password;

    /** 确认密码（须与密码一致，跨字段校验在业务层执行） */
    @ApiModelProperty(value = "确认密码", required = true)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 手机号（中国大陆 11 位手机号） */
    @ApiModelProperty(value = "手机号", required = true, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 昵称 */
    @ApiModelProperty(value = "昵称", required = true, example = "张三")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;
}
