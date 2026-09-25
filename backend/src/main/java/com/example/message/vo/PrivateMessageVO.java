package com.example.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天记录中的单条私信（展示对象）
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class PrivateMessageVO {

    /** 私信 ID */
    private Long id;

    /** 发送者 ID */
    private Long fromUserId;

    /** 接收者 ID */
    private Long toUserId;

    /** 关联商品 ID（普通私信为空） */
    private Long productId;

    /** 接收方是否已读：0 未读，1 已读 */
    private Integer isRead;

    /** 私信内容 */
    private String content;

    /** 是否由当前登录用户发出（前端据此决定气泡左右） */
    private Boolean self;

    /** 创建时间 */
    private LocalDateTime createTime;
}
