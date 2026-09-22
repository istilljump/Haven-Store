package com.example.common;

/**
 * Redis Key 统一管理常量
 * <p>
 * 所有模块的缓存 Key 前缀集中定义，避免各处硬编码字符串导致冲突
 *
 * @author ZCode
 * @date 2026/09/21
 */
public final class RedisKeyConst {

    /** 用户信息缓存 Key 前缀，完整格式：user:info:{userId} */
    public static final String USER_INFO_KEY = "user:info:";

    /** 系统设置缓存 Key（管理后台「系统设置」页的持久化位置，全系统共用一份） */
    public static final String SYSTEM_SETTINGS_KEY = "system:settings";

    /**
     * 常量类禁止实例化
     */
    private RedisKeyConst() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
