package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评论展示对象
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "CommentVO", description = "商品评论")
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评论 ID */
    @ApiModelProperty("评论ID")
    private Long id;

    /** 评论内容 */
    @ApiModelProperty("评论内容")
    private String content;

    /** 评分 1-5 */
    @ApiModelProperty("评分")
    private Integer rating;

    /** 评论人用户 ID */
    @ApiModelProperty("评论人用户ID")
    private Long userId;

    /** 评论人用户名 */
    @ApiModelProperty("评论人用户名")
    private String username;

    /** 评论人头像 */
    @ApiModelProperty("评论人头像")
    private String avatar;

    /** 评论时间 */
    @ApiModelProperty("评论时间")
    private LocalDateTime createTime;
}
