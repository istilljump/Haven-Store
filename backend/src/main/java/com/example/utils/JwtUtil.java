package com.example.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（基于 JJWT 0.11.5 实现）
 * <p>
 * Token 载荷中以 Subject 形式保存用户 ID，签名算法为 HS256
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Slf4j
@Component
public class JwtUtil {

    /** JWT 签名密钥（从配置文件读取，HS256 算法要求不少于 32 字节） */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 有效期（毫秒），从配置文件读取 */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 根据用户 ID 生成 JWT Token
     *
     * @param userId 用户 ID
     * @return 签名后的 Token 字符串
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                // 将用户 ID 作为 Token 主题（Subject）保存
                .setSubject(String.valueOf(userId))
                // Token 签发时间
                .setIssuedAt(now)
                // Token 过期时间
                .setExpiration(expireDate)
                // 使用 HS256 算法与自定义密钥进行签名
                .signWith(Keys.hmacShaKeyFor(getSecretKeyBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Token 中解析用户 ID
     * <p>
     * 注意：调用前应先通过 validateToken 确认 Token 有效，否则将抛出 JwtException
     *
     * @param token Token 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKeyBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 校验 Token 是否有效（签名正确且未过期）
     *
     * @param token Token 字符串
     * @return true 有效；false 无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKeyBytes())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期：{}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token 签名或格式无效：{}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token 为空或格式错误：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取签名密钥的字节数组（HS256 要求不少于 32 字节）
     *
     * @return 密钥字节数组
     */
    private byte[] getSecretKeyBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
