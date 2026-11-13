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

    /** 议价报价金额（报价消息非空，前端渲染报价卡片） */
    private java.math.BigDecimal offerPrice;

    /** 报价失效时间（前端倒计时用；已失效/已接受也有值） */
    private LocalDateTime offerExpireTime;

    /** 报价状态：0 待处理 / 1 已接受 / 2 已失效；非报价消息为 NULL */
    private Integer offerStatus;

    /** 是否由当前登录用户发出（前端据此决定气泡左右） */
    private Boolean self;

    /** 创建时间 */
    private LocalDateTime createTime;
}
