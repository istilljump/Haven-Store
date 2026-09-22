package com.example.product.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 发表商品评论入参
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "CommentAddDTO", description = "发表评论入参")
public class CommentAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评论内容 */
    @ApiModelProperty(value = "评论内容", required = true)
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 2, max = 500, message = "评论内容长度须为2-500个字符")
    private String content;

    /** 评分 1-5 */
    @ApiModelProperty(value = "评分：1-5", required = true, example = "5")
    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 5, message = "评分最高为5")
    private Integer rating;
}
