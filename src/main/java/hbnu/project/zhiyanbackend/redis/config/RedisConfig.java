package hbnu.project.zhiyanbackend.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 配置 Redis 序列化器，支持 Java 8 日期时间类型
 *
 * @author ErgouTree
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 创建自定义的ObjectMapper
        ObjectMapper objectMapper = createObjectMapper();

        // 修复：参数顺序应为 Class<T> 在前，ObjectMapper 在后
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class, objectMapper);

        // 设置Key的序列化方式为String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // 设置Value的序列化方式为JSON
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 创建配置好的ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册JavaTimeModule以支持LocalDateTime、LocalDate等Java 8日期时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用将日期序列化为时间戳，使用ISO-8601格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 启用类型信息，以便反序列化时能正确还原对象类型
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 忽略未知属性，提高兼容性
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 空值不序列化
        objectMapper.setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        return objectMapper;
    }

    /**
     * 对于Spring Data Redis 3.2+，也可以使用RedisSerializationContext配置
     */
    /*
    @Bean
    public RedisSerializationContext.SerializationPair<Object> jsonSerializationPair() {
        ObjectMapper objectMapper = createObjectMapper();
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class, objectMapper);
        return RedisSerializationContext.SerializationPair.fromSerializer(serializer);
    }
    */
}