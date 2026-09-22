package com.example.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局序列化配置类
 * <p>
 * 将 LocalDateTime 类型统一序列化为 yyyy-MM-dd HH:mm:ss 格式字符串，
 * 前端无需再做时间格式化处理
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Configuration
public class JacksonConfig {

    /** 全局日期时间展示格式 */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 定制全局 ObjectMapper：注册 LocalDateTime 的序列化与反序列化器
     *
     * @return ObjectMapper 构建定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> builder
                // LocalDateTime 序列化：LocalDateTime -> "yyyy-MM-dd HH:mm:ss"
                .serializers(new LocalDateTimeSerializer(dateTimeFormatter))
                // LocalDateTime 反序列化："yyyy-MM-dd HH:mm:ss" -> LocalDateTime
                .deserializers(new LocalDateTimeDeserializer(dateTimeFormatter));
    }
}
