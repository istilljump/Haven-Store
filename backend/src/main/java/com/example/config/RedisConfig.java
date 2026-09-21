package com.example.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * <p>
 * 自定义 RedisTemplate 序列化策略：
 * Key 使用 String 序列化（可读性好、便于运维排查），
 * Value 使用 Jackson JSON 序列化（跨语言、可读、支持对象与 JDK8 时间类型）
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate Bean
     *
     * @param connectionFactory Redis 连接工厂（由 Spring Boot 自动装配）
     * @return 配置好序列化策略的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置 Redis 连接工厂
        redisTemplate.setConnectionFactory(connectionFactory);

        // Key 与 HashKey 统一使用 String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // Value 与 HashValue 统一使用 Jackson JSON 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = buildJsonSerializer();
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 使序列化配置生效
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 构建 Jackson JSON 序列化器
     * <p>
     * 开启类型信息携带（反序列化时可还原为原对象类型），
     * 并注册 Java 8 时间模块以支持 LocalDateTime 等类型的序列化
     *
     * @return Jackson JSON 序列化器
     */
    private GenericJackson2JsonRedisSerializer buildJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 所有属性可见性设为 ANY，支持对象私有属性序列化
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 序列化时携带对象类型信息，反序列化可还原为原类型
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        // 注册 Java 8 时间模块，支持 LocalDateTime 类型
        objectMapper.registerModule(new JavaTimeModule());
        // 日期时间序列化为可读字符串格式，而非时间戳数组
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
