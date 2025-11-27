package hbnu.project.zhiyanbackend.redis.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Redis使用Jackson序列化
 *
 * @author yui
 */
public class Jackson2JsonRedisSerializer<T> implements RedisSerializer<T> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    // 提供ObjectMapper的getter方法
    @Getter
    private final ObjectMapper objectMapper;
    private final Class<T> clazz;

    /**
     * 默认的ObjectMapper配置
     */
    public static ObjectMapper getDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. 配置多态类型处理（安全的方式替代activateDefaultTyping）
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 2. 忽略未知属性，提高兼容性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 3. 空值处理（使用setDefaultPropertyInclusion替代已弃用的方法）
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

        // 4. 禁用日期时间的时间戳序列化
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 5. 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }

    public Jackson2JsonRedisSerializer(Class<T> clazz) {
        this(clazz, getDefaultObjectMapper());
    }

    public Jackson2JsonRedisSerializer(Class<T> clazz, ObjectMapper objectMapper) {
        super();
        Assert.notNull(clazz, "序列化的目标类不能为null");
        Assert.notNull(objectMapper, "ObjectMapper不能为null");
        this.clazz = clazz;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            // 将对象序列化为JSON字节数组，包含类型信息
            return objectMapper.writeValueAsBytes(t);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Could not serialize object to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes.length == 0) {
            return null;
        }
        try {
            // 将JSON字节数组反序列化为指定类型对象
            return objectMapper.readValue(bytes, clazz);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize JSON bytes: " + e.getMessage(), e);
        }
    }

}