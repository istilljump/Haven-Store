package com.example.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 面交码确认入参（轮8 业务迭代新增）
 *
 * @author ZCode
 * @date 2026/11
 */
@Data
public class ConfirmCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 6 位数字面交确认码（买家订单详情可见） */
    @NotBlank(message = "请输入面交确认码")
    @Pattern(regexp = "\\d{6}", message = "确认码为 6 位数字")
    private String code;
}
