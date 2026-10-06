package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 评论状态变更入参（管理后台）
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "CommentStatusDTO", description = "评论状态变更入参")
public class CommentStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标状态：1 正常，0 隐藏 */
    @ApiModelProperty(value = "目标状态：1正常 0隐藏", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
