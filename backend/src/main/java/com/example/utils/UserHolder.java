package com.example.utils;

import com.example.user.entity.User;

/**
 * 登录用户上下文工具类（基于 ThreadLocal 保存当前请求的用户 ID 与用户实体）
 * <p>
 * JWT 拦截器鉴权通过后调用 {@link #setUserId(Long)}/{@link #setUser(User)} 写入，
 * 业务代码任意位置通过 {@link #getUserId()}/{@link #getUser()} 获取当前登录用户信息，
 * 请求结束时拦截器回调 {@link #remove()} 清理，防止线程池复用导致的数据串用与内存泄漏
 *
 * @author ZCode
 * @date 2026/09/21
 */
public class UserHolder {

    /** 保存当前登录用户 ID 的 ThreadLocal 变量 */
    private static final ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 保存当前登录用户实体的 ThreadLocal 变量
     * <p>
     * 由 JWT 拦截器在鉴权时统一加载（Redis 缓存优先），同一请求内的
     * 状态校验、角色校验等直接复用，避免重复查库
     */
    private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前登录用户 ID（由 JWT 拦截器在鉴权成功后调用）
     *
     * @param userId 用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_THREAD_LOCAL.set(userId);
    }

    /**
     * 设置当前登录用户实体（由 JWT 拦截器在鉴权成功后调用）
     *
     * @param user 用户实体（密码字段已剔除）
     */
    public static void setUser(User user) {
        USER_THREAD_LOCAL.set(user);
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
     * 获取当前登录用户实体
     *
     * @return 用户实体；请求未经过鉴权拦截器时返回 null
     */
    public static User getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 清理 ThreadLocal（必须在请求结束时调用，防止内存泄漏）
     */
    public static void remove() {
        USER_ID_THREAD_LOCAL.remove();
        USER_THREAD_LOCAL.remove();
    }
}
