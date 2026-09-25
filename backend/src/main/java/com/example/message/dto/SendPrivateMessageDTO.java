package com.example.message.dto;

import com.example.message.constant.PrivateMessageConstant;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发送私信入参
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class SendPrivateMessageDTO {

    /** 接收者 ID */
    @NotNull(message = "接收者不能为空")
    private Long toUserId;

    /**
     * 关联商品 ID
     * <p>
     * 商品咨询时填写；普通私信不传，为空
     */
    private Long productId;

    /** 私信内容 */
    @NotBlank(message = "私信内容不能为空")
    @Size(max = PrivateMessageConstant.CONTENT_MAX_LENGTH, message = "私信内容不能超过 500 字")
    private String content;
}
