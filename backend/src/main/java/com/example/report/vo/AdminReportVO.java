package com.example.report.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报列表项（管理后台）
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "AdminReportVO", description = "管理后台举报列表项")
public class AdminReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 举报 ID */
    @ApiModelProperty("举报ID")
    private Long id;

    /** 举报人用户 ID */
    @ApiModelProperty("举报人用户ID")
    private Long reporterId;

    /** 举报人用户名 */
    @ApiModelProperty("举报人用户名")
    private String reporterUsername;

    /** 举报对象类型：product 商品 / comment 评论 */
    @ApiModelProperty("举报对象类型")
    private String targetType;

    /** 举报对象 ID */
    @ApiModelProperty("举报对象ID")
    private Long targetId;

    /** 被举报对象摘要（商品标题或评论内容） */
    @ApiModelProperty("被举报对象摘要")
    private String targetSummary;

    /** 被举报对象当前状态：商品状态或评论状态；对象已被删除时为 null */
    @ApiModelProperty("被举报对象当前状态")
    private Integer targetStatus;

    /** 举报原因 */
    @ApiModelProperty("举报原因")
    private String reason;

    /** 补充说明 */
    @ApiModelProperty("补充说明")
    private String description;

    /** 处理状态：0 待处理，1 已处理，2 已驳回 */
    @ApiModelProperty("处理状态")
    private Integer status;

    /** 处理动作 */
    @ApiModelProperty("处理动作")
    private String handleAction;

    /** 处理备注 */
    @ApiModelProperty("处理备注")
    private String handleNote;

    /** 举报时间 */
    @ApiModelProperty("举报时间")
    private LocalDateTime createTime;
}
