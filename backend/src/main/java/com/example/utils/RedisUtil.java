package com.example.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具类（封装 RedisTemplate 常用操作）
 * <p>
 * 统一缓存读写入口，便于后续扩展（如统一加前缀、监控埋点等）
 *
 * @author ZCode
 * @date 2026/09/21
 */
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

    /**
     * 判断缓存键是否存在
     *
     * @param key 缓存键
     * @return true 存在；false 不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
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
