package com.example.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项（展示对象）
 * <p>
 * 一个会话由「对方用户 + 关联商品」唯一确定：普通私信的商品字段为空，
 * 商品咨询则带回被咨询商品的标题与封面，便于在列表里区分
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class ConversationVO {

    /** 对方用户 ID */
    private Long peerId;

    /** 对方昵称（未设置昵称时回落为用户名） */
    private String peerName;

    /** 对方头像 */
    private String peerAvatar;

    /** 关联商品 ID（普通私信为空） */
    private Long productId;

    /** 关联商品标题 */
    private String productTitle;

    /** 关联商品封面图 */
    private String productCoverImage;

    /** 最近一条私信内容 */
    private String lastContent;

    /** 最近一条私信时间 */
    private LocalDateTime lastTime;

    /** 我尚未读的该会话私信条数 */
    private Integer unreadCount;
}
