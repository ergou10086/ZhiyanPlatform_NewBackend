package hbnu.project.zhiyanbackend.basic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson配置类
 * 解决雪花ID精度丢失问题：将Long类型ID序列化为String
 * 支持Java 8日期时间类型序列化
 * <p>
 * 提供两种配置方式：
 * 1. 全局配置：所有Long类型都序列化为String（默认启用）
 * 2. 注解配置：只有使用@LongToString注解的字段才序列化为String
 * <p>
 * 可通过配置项 zhiyan.jackson.long-to-string-global=false 来禁用全局配置
 *
 * @author ErgouTree
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 使用 Jackson2ObjectMapperBuilderCustomizer 定制全局 ObjectMapper
     * 统一配置 Long 转 String 以及 Java 8 日期时间序列化
     * <p>
     * 通过配置项 zhiyan.jackson.long-to-string-global=false 可以关闭全局 Long->String，
     * 此时仍可通过 @LongToString 注解对单字段生效
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "spring.jackson",
            name = "long-to-string-global",
            havingValue = "true",
            matchIfMissing = true
    )
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. Long 转 String 模块（避免前端 JS 精度丢失）
        SimpleModule longToStringModule = new SimpleModule("LongToStringModule");
        longToStringModule.addSerializer(Long.class, ToStringSerializer.instance);
        longToStringModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 2. Java 8 日期时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));

        // LocalDate
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));

        // LocalTime
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
        javaTimeModule.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

        // 注册模块并关闭时间戳
        objectMapper.registerModule(longToStringModule);
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
