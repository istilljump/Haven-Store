package com.example.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信实体（对应数据库 private_message 表）
 * <p>
 * 商品咨询复用本表：{@code productId} 非空即表示这是一条针对该商品的咨询，
 * 为空则是普通的用户间私信
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
@TableName("private_message")
public class PrivateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 私信 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话键：由双方用户 ID 与商品 ID 拼成，值相同即属同一会话 */
    private String conversationKey;

    /** 发送者 ID */
    private Long fromUserId;

    /** 接收者 ID */
    private Long toUserId;

    /** 关联商品 ID（仅商品咨询填写） */
    private Long productId;

    /** 私信内容 */
    private String content;

    /** 接收方是否已读：0 未读，1 已读 */
    private Integer isRead;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入与更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
