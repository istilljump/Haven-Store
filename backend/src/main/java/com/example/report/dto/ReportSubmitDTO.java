package com.example.report.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 提交举报入参
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@ApiModel(value = "ReportSubmitDTO", description = "提交举报入参")
public class ReportSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 举报对象类型：product 商品 / comment 评论 */
    @ApiModelProperty(value = "举报对象类型", required = true, example = "product")
    @NotBlank(message = "举报对象类型不能为空")
    @Pattern(regexp = "^(product|comment)$", message = "举报对象类型不合法")
    private String targetType;

    /** 举报对象 ID */
    @ApiModelProperty(value = "举报对象ID", required = true, example = "1")
    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    /** 举报原因 */
    @ApiModelProperty(value = "举报原因", required = true, example = "涉嫌欺诈")
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 30, message = "举报原因长度不能超过30个字符")
    private String reason;

    /** 补充说明 */
    @ApiModelProperty(value = "补充说明", example = "商品描述与实物不符")
    @Size(max = 255, message = "补充说明长度不能超过255个字符")
    private String description;
}
