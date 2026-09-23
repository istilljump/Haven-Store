package com.example.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具类（封装 RedisTemplate 常用操作）
 * <p>
 * 分两类方法：
 * <ul>
 *   <li>{@code set/get/delete}：失败时向上抛异常，用于「缓存即数据源」的场景（如系统设置）</li>
 *   <li>{@code setQuietly/getQuietly/deleteQuietly}：失败只记警告日志，用于纯缓存场景
 *       （如登录后缓存用户信息）——缓存不可用不应该让登录、资料修改这类核心流程整体失败</li>
 * </ul>
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    /** Redis 操作模板（由 RedisConfig 配置：Key 为 String 序列化、Value 为 JSON 序列化） */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入缓存（不设置过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入缓存并设置过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时长
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 读取缓存
     *
     * @param <T> 值类型
     * @param key 缓存键
     * @return 缓存值；键不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return true 删除成功；false 键不存在
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    // ==================== 纯缓存场景的安全版本 ====================

    /**
     * 写入缓存（失败只记日志，不影响业务流程）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时长
     * @param unit    时间单位
     */
    public void setQuietly(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("写入缓存失败，已跳过（不影响业务流程），key：{}，原因：{}", key, e.getMessage());
        }
    }

    /**
     * 读取缓存（失败只记日志，返回 null 由调用方回源数据库）
     *
     * @param <T> 值类型
     * @param key 缓存键
     * @return 缓存值；键不存在或缓存不可用时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getQuietly(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取缓存失败，已回源处理，key：{}，原因：{}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 删除缓存（失败只记日志，不影响业务流程）
     *
     * @param key 缓存键
     */
    public void deleteQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("清除缓存失败，已跳过，key：{}，原因：{}", key, e.getMessage());
        }
    }

    /**
     * 判断缓存是否可用（供健康检查使用）
     *
     * @return true 表示 Redis 连接正常
     */
    public boolean isAvailable() {
        try {
            redisTemplate.hasKey("__health_check__");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 为已存在的缓存键设置过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时长
     * @param unit    时间单位
     * @return true 设置成功；false 键不存在
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }
}
