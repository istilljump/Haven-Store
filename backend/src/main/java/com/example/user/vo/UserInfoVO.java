package com.example.user.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前登录用户信息（个人中心展示用，脱敏）
 * <p>
 * 不含密码字段；与登录返回信息（LoginUserVO）的区别是这里包含手机号、邮箱、
 * 个人简介等完整资料，用于个人中心的基本信息表单
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "UserInfoVO", description = "当前登录用户信息")
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @ApiModelProperty("用户ID")
    private Long id;

    /** 用户名（登录账号，不可修改） */
    @ApiModelProperty("用户名")
    private String username;

    /** 昵称 */
    @ApiModelProperty("昵称")
    private String nickname;

    /** 手机号 */
    @ApiModelProperty("手机号")
    private String phone;

    /** 邮箱 */
    @ApiModelProperty("邮箱")
    private String email;

    /** 个人简介 */
    @ApiModelProperty("个人简介")
    private String bio;

    /** 头像 URL */
    @ApiModelProperty("头像URL")
    private String avatar;

    /** 账号状态：1 正常，0 禁用 */
    @ApiModelProperty("账号状态：1正常 0禁用")
    private Integer status;

    /** 是否管理员（由 role 换算，前端据此显示管理后台入口） */
    @ApiModelProperty("是否管理员")
    private Boolean isAdmin;

    /** 注册时间 */
    @ApiModelProperty("注册时间")
    private LocalDateTime createTime;
}
