package com.example.user.constant;

/**
 * 用户模块常量定义
 *
 * @author ZCode
 * @date 2026/09/21
 */
public final class UserConstant {

    /** 注册用户默认头像 URL（Element Plus 官方示例头像，保证默认头像长期可访问） */
    public static final String DEFAULT_AVATAR = "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png";

    /** 角色：普通用户（注册时的默认角色） */
    public static final Integer ROLE_USER = 0;

    /** 角色：管理员（可登录管理后台） */
    public static final Integer ROLE_ADMIN = 1;

    /**
     * 常量类禁止实例化
     */
    private UserConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
