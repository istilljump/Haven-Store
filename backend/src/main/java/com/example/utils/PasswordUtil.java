package com.example.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类（基于 Spring Security 的 BCrypt 算法）
 * <p>
 * BCrypt 自带随机盐，同一明文每次加密结果不同，无法通过彩虹表逆推明文；
 * 密码明文只出现在入参与比对过程，禁止写入日志与返回结果
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Component
public class PasswordUtil {

    /** BCrypt 加密器（线程安全，静态复用即可） */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 对明文密码进行 BCrypt 加密
     *
     * @param rawPassword 明文密码
     * @return 加密后的密文（固定 60 位字符串）
     */
    public String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验明文密码与密文密码是否匹配
     *
     * @param rawPassword     前端传入的明文密码
     * @param encodedPassword 数据库存储的密文密码
     * @return true 匹配；false 不匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
