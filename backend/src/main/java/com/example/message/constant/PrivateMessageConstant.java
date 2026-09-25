package com.example.message.constant;

/**
 * 私信模块常量
 * <p>
 * 系统内禁止出现魔法值，私信相关的固定取值一律引用本类
 *
 * @author ZCode
 * @date 2026/09/25
 */
public final class PrivateMessageConstant {

    /** 未读 */
    public static final Integer UNREAD = 0;

    /** 已读 */
    public static final Integer READ = 1;

    /** 会话键分隔符 */
    public static final String CONVERSATION_KEY_SEPARATOR = "-";

    /** 普通私信的会话键中代表「无关联商品」的占位值（商品ID 自增从 1 开始，0 不会冲突） */
    public static final Long NO_PRODUCT_PLACEHOLDER = 0L;

    /** 私信内容最大长度（与 private_message.content 列宽保持一致） */
    public static final int CONTENT_MAX_LENGTH = 500;

    /** 未读条数为 0：会话里没有等着我读的消息 */
    public static final int NO_UNREAD = 0;

    private PrivateMessageConstant() {
    }
}
