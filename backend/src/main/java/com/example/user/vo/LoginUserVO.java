package com.example.user.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录成功返回信息（脱敏视图对象，不含密码、手机号等敏感字段）
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "LoginUserVO", description = "登录成功返回信息")
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @ApiModelProperty("用户ID")
    private Long userId;

    /** 用户名 */
    @ApiModelProperty("用户名")
    private String username;

    /** 昵称 */
    @ApiModelProperty("昵称")
    private String nickname;

    /** 头像 URL */
    @ApiModelProperty("头像URL")
    private String avatar;

    /** JWT Token（后续请求放入请求头：Authorization: Bearer {token}） */
    @ApiModelProperty("登录令牌Token")
    private String token;
}
