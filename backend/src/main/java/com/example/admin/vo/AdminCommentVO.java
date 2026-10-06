package com.example.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理后台评论列表项
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "AdminCommentVO", description = "管理后台评论列表项")
public class AdminCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评论 ID */
    @ApiModelProperty("评论ID")
    private Long id;

    /** 商品 ID */
    @ApiModelProperty("商品ID")
    private Long productId;

    /** 商品标题 */
    @ApiModelProperty("商品标题")
    private String productTitle;

    /** 评论人用户 ID */
    @ApiModelProperty("评论人用户ID")
    private Long userId;

    /** 评论人名称 */
    @ApiModelProperty("评论人名称")
    private String username;

    /** 评论内容 */
    @ApiModelProperty("评论内容")
    private String content;

    /** 评分 1-5 */
    @ApiModelProperty("评分")
    private Integer rating;

    /** 状态：1 正常，0 隐藏 */
    @ApiModelProperty("状态：1正常 0隐藏")
    private Integer status;

    /** 评论时间 */
    @ApiModelProperty("评论时间")
    private LocalDateTime createTime;
}
