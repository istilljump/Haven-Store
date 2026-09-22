package com.example.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理后台用户列表项（脱敏）
 * <p>
 * 注意：不含密码字段；角色以布尔值 isAdmin 输出，
 * 前端表格「角色」列直接读该字段
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "AdminUserVO", description = "管理后台用户列表项")
public class AdminUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @ApiModelProperty("用户ID")
    private Long id;

    /** 用户名 */
    @ApiModelProperty("用户名")
    private String username;

    /** 昵称 */
    @ApiModelProperty("昵称")
    private String nickname;

    /** 手机号 */
    @ApiModelProperty("手机号")
    private String phone;

    /** 头像 URL */
    @ApiModelProperty("头像URL")
    private String avatar;

    /** 账号状态：1 正常，0 禁用 */
    @ApiModelProperty("账号状态：1正常 0禁用")
    private Integer status;

    /** 是否管理员（由 role 字段换算） */
    @ApiModelProperty("是否管理员")
    private Boolean isAdmin;

    /** 注册时间 */
    @ApiModelProperty("注册时间")
    private LocalDateTime createTime;

    /** 最后更新时间 */
    @ApiModelProperty("最后更新时间")
    private LocalDateTime updateTime;
}
