package com.example.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置
 * <p>
 * 启用 Spring Cache 并显式声明基于 Redis 的缓存管理器：
 * Key 用 String 序列化（可读、便于运维排查），Value 用 JSON 序列化
 * （替代 Spring Boot 默认的 JDK 序列化，跨语言且可读）；
 * 缓存条目统一带 TTL，防止只写不删导致的长期脏数据
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 分类缓存名（前台发布页/筛选下拉共用，管理端变更时整组驱逐） */
    public static final String CACHE_CATEGORIES = "categories";

    /** AI 估价结果缓存名 */
    public static final String CACHE_ESTIMATIONS = "productEstimations";

    /**
     * 基于 Redis 的缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 缓存 Value 与 RedisTemplate 保持一致的 JSON 序列化策略
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(buildObjectMapper());
        RedisSerializationContext.SerializationPair<Object> valuePair =
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);
        RedisSerializationContext.SerializationPair<String> keyPair =
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

        // 各缓存名的独立 TTL
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_CATEGORIES, baseConfig(keyPair, valuePair).entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_ESTIMATIONS, baseConfig(keyPair, valuePair).entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig(keyPair, valuePair).entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * 缓存条目的基础序列化配置（空值不缓存，防止缓存穿透）
     */
    private RedisCacheConfiguration baseConfig(RedisSerializationContext.SerializationPair<String> keyPair,
                                               RedisSerializationContext.SerializationPair<Object> valuePair) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(keyPair)
                .serializeValuesWith(valuePair)
                .disableCachingNullValues();
    }

    /**
     * 缓存专用的 ObjectMapper：与 RedisConfig 相同的白名单类型校验 + 时间模块
     */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 反序列化类型白名单：本项目业务类 + 集合/时间/数值等基础类型
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.math.")
                .build();
        objectMapper.activateDefaultTyping(typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 容忍未知字段：Result 等对象带有 getter 派生属性（如 success），
        // 序列化会写出去，反序列化时没有对应 setter，不能因此报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
