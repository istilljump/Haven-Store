package com.example.report.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 举报处理入参（管理员）
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "ReportHandleDTO", description = "举报处理入参")
public class ReportHandleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 处理动作：
     * takeDownProduct 下架被举报商品 / hideComment 隐藏被举报评论 / dismiss 驳回举报
     */
    @ApiModelProperty(value = "处理动作", required = true, example = "dismiss")
    @NotBlank(message = "处理动作不能为空")
    @Pattern(regexp = "^(takeDownProduct|hideComment|dismiss)$", message = "处理动作不合法")
    private String action;

    /** 处理备注 */
    @ApiModelProperty(value = "处理备注", example = "已核实并下架")
    @Size(max = 255, message = "处理备注长度不能超过255个字符")
    private String note;
}
