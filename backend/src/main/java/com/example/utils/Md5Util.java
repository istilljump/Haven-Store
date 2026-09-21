package com.example.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 摘要工具类
 * <p>
 * 仅适用于非安全场景：生成缓存 Key（如 AI 估价入参摘要）、接口签名、
 * 文件/字符串摘要比对等对单向强度无要求的场合。
 * 【安全红线】MD5 属于快速哈希算法且存在彩虹表，禁止用于密码加密或任何
 * 安全凭证的存储与校验；密码必须使用 {@link PasswordUtil}（BCrypt，自带随机盐）
 *
 * @author ZCode
 * @date 2026/09/21
 */
public class Md5Util {

    /** 十六进制小写字符表 */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /** 工具类禁止实例化 */
    private Md5Util() {
    }

    /**
     * 计算字符串的 MD5 摘要
     *
     * @param text 原始字符串
     * @return 32 位小写十六进制摘要串
     */
    public static String md5Hex(String text) {
        if (text == null) {
            throw new IllegalArgumentException("待摘要内容不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // JDK 规范保证 MD5 算法可用，此分支理论上不可达
            throw new IllegalStateException("当前运行环境不支持 MD5 算法", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串（长度为字节数组两倍）
     */
    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            chars[i * 2] = HEX_CHARS[value >>> 4];
            chars[i * 2 + 1] = HEX_CHARS[value & 0x0F];
        }
        return new String(chars);
    }
}
