package com.example.message.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 发送消息DTO
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
public class SendMessageDTO {
    
    /** 接收者ID */
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;
    
    /** 消息类型（1: 系统通知，2: 公告，3: 交易消息，4: 其他） */
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;
    
    /** 消息标题 */
    @NotBlank(message = "消息标题不能为空")
    private String title;
    
    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}