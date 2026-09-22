package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理后台群发系统消息入参
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "AdminMessageSendDTO", description = "系统消息群发入参")
public class AdminMessageSendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息标题 */
    @ApiModelProperty(value = "消息标题", required = true, example = "系统维护通知")
    @NotBlank(message = "消息标题不能为空")
    @Size(max = 50, message = "消息标题长度不能超过50个字符")
    private String title;

    /** 消息内容 */
    @ApiModelProperty(value = "消息内容", required = true)
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容长度不能超过500个字符")
    private String content;

    /** 接收群体：all 所有用户 / admin 仅管理员 / user 仅普通用户 */
    @ApiModelProperty(value = "接收群体：all/admin/user", required = true, example = "all")
    @NotBlank(message = "接收用户不能为空")
    private String userType;
}
