package com.example.common;

/**
 * 系统通用常量定义
 * <p>
 * 系统内禁止出现魔法值，跨模块通用常量统一定义在本类
 *
 * @author ZCode
 * @date 2026/09/21
 */
public final class Constants {

    /** 请求头中 Token 的前缀标识（完整格式：Bearer {token}） */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 常量类禁止实例化
     */
    private Constants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
