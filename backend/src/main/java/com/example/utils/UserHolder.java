package com.example.utils;

/**
 * 登录用户上下文工具类（基于 ThreadLocal 保存当前请求的用户 ID）
 * <p>
 * JWT 拦截器鉴权通过后调用 {@link #setUserId(Long)} 写入，
 * 业务代码任意位置通过 {@link #getUserId()} 获取当前登录用户 ID，
 * 请求结束时拦截器回调 {@link #remove()} 清理，防止线程池复用导致的数据串用与内存泄漏
 *
 * @author ZCode
 * @date 2026/09/21
 */
public class UserHolder {

    /** 保存当前登录用户 ID 的 ThreadLocal 变量 */
    private static final ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前登录用户 ID（由 JWT 拦截器在鉴权成功后调用）
     *
     * @param userId 用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_THREAD_LOCAL.set(userId);
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID；请求未经过鉴权拦截器时返回 null
     */
    public static Long getUserId() {
        return USER_ID_THREAD_LOCAL.get();
    }

    /**
     * 清理 ThreadLocal（必须在请求结束时调用，防止内存泄漏）
     */
    public static void remove() {
        USER_ID_THREAD_LOCAL.remove();
    }
}
