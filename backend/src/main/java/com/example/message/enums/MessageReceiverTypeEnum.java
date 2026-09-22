package com.example.message.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息接收群体枚举
 * <p>
 * 管理后台群发系统消息时用于指定接收范围，同时决定消息落库的 message_type。
 * 与数据库 system_message.receiver_type / message_type 两个字段对应
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Getter
@AllArgsConstructor
public enum MessageReceiverTypeEnum {

    /** 所有用户（对应公告类型的消息） */
    ALL("all", "所有用户", 2),

    /** 仅管理员 */
    ADMIN("admin", "仅管理员", 4),

    /** 仅普通用户（对应系统通知类型的消息） */
    USER("user", "仅普通用户", 1);

    /** 接收群体标识（与前端表单取值、数据库 receiver_type 一致） */
    private final String code;

    /** 群体描述 */
    private final String desc;

    /** 落库使用的消息类型：1 系统通知，2 公告，3 交易消息，4 其他 */
    private final Integer messageType;

    /**
     * 根据接收群体标识查找枚举
     *
     * @param code 接收群体标识
     * @return 匹配的枚举；无匹配时返回 null
     */
    public static MessageReceiverTypeEnum of(String code) {
        for (MessageReceiverTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }
}
