package com.example.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类（对应数据库 user 表）
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "User", description = "用户实体")
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（主键，数据库自增） */
    @ApiModelProperty("用户ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录账号，全局唯一） */
    @ApiModelProperty("用户名")
    private String username;

    /** 密码（BCrypt 密文存储；任何接口的返回结果与日志中均不允许出现该字段） */
    @ApiModelProperty(value = "密码（密文）", hidden = true)
    private String password;

    /** 手机号（全局唯一） */
    @ApiModelProperty("手机号")
    private String phone;

    /** 昵称 */
    @ApiModelProperty("昵称")
    private String nickname;

    /** 头像 URL */
    @ApiModelProperty("头像URL")
    private String avatar;

    /** 创建时间（插入时由 MetaObjectHandler 自动填充） */
    @ApiModelProperty("创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入与更新时由 MetaObjectHandler 自动填充） */
    @ApiModelProperty("更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 账号状态：1 正常，0 禁用（取值见 UserStatusEnum） */
    @ApiModelProperty("账号状态：1正常 0禁用")
    private Integer status;

    /** 角色：1 管理员，0 普通用户（取值见 UserConstant.ROLE_*） */
    @ApiModelProperty("角色：1管理员 0普通用户")
    private Integer role;
}
