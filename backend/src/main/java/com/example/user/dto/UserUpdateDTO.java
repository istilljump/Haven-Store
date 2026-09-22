package com.example.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户资料修改入参
 * <p>
 * 用户名是登录账号，不允许修改，因此不在入参中
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "UserUpdateDTO", description = "用户资料修改入参")
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 昵称 */
    @ApiModelProperty(value = "昵称", example = "张三")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /** 手机号 */
    @ApiModelProperty(value = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱 */
    @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /** 个人简介 */
    @ApiModelProperty(value = "个人简介")
    @Size(max = 255, message = "个人简介长度不能超过255个字符")
    private String bio;
}
