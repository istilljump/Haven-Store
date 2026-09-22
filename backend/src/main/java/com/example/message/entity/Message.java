package com.example.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统消息实体类
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@TableName("system_message")
public class Message {
    
    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 接收者ID */
    private Long receiverId;
    
    /** 消息类型（1: 系统通知，2: 公告，3: 交易消息，4: 其他） */
    private Integer messageType;
    
    /** 消息标题 */
    private String title;
    
    /** 消息内容 */
    private String content;
    
    /** 是否已读（0: 未读，1: 已读） */
    private Integer isRead;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}