package hbnu.project.zhiyanbackend.redis.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Redis 工具类 - 基于 Spring Data Redis
 *
 * @author yui
 * @rewrite ErgouTree
 */
@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings(value = {"unchecked"})
public class RedisUtils {

    /**
     * 用于存储消息监听器容器和订阅关系
     */
    private static final Map<String, RedisMessageListenerContainer> LISTENER_CONTAINERS = new ConcurrentHashMap<>();

    /**
     * -- GETTER --
     * 获取RedisTemplate实例
     */
    @Getter
    private static RedisTemplate<String, Object> redisTemplate;

    /**
     * -- GETTER --
     * 获取StringRedisTemplate实例
     */
    @Getter
    private static StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtils.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        RedisUtils.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 限流 - 基于 Redis Lua 脚本实现令牌桶算法
     *
     * @param key         限流key
     * @param maxRequests 最大请求数
     * @param timeWindow  时间窗口（秒）
     * @return -1 表示失败，否则返回剩余令牌数
     */
    public static long rateLimiter(String key, int maxRequests, int timeWindow) {
        String luaScript =
                """
                        local key = KEYS[1]
                        local max_requests = tonumber(ARGV[1])
                        local time_window = tonumber(ARGV[2])
                        local current_time = tonumber(ARGV[3])
                        
                        local last_time = redis.call('hget', key, 'last_time')
                        local tokens = redis.call('hget', key, 'tokens')
                        
                        if last_time == false then
                            tokens = max_requests
                            last_time = current_time
                        else
                            local time_passed = current_time - tonumber(last_time)
                            local refill_tokens = math.floor(time_passed * max_requests / time_window)
                            tokens = math.min(max_requests, tonumber(tokens) + refill_tokens)
                            last_time = current_time
                        end
                        
                        if tokens >= 1 then
                            tokens = tokens - 1
                            redis.call('hset', key, 'last_time', last_time)
                            redis.call('hset', key, 'tokens', tokens)
                            redis.call('expire', key, time_window)
                            return tokens
                        else
                            return -1
                        end""";

        RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(key),
                maxRequests, timeWindow, System.currentTimeMillis() / 1000);

        return result != null ? result : -1L;
    }

    /**
     * 发布通道消息
     *
     * @param channelKey 通道key
     * @param msg        发送数据
     * @param consumer   自定义处理
     */
    public static <T> void publish(String channelKey, T msg, Consumer<T> consumer) {
        redisTemplate.convertAndSend(channelKey, msg);
        consumer.accept(msg);
    }

    /**
     * 发布消息到指定的频道
     *
     * @param channelKey 通道key
     * @param msg        发送数据
     */
    public static <T> void publish(String channelKey, T msg) {
        redisTemplate.convertAndSend(channelKey, msg);
    }

    /**
     * 订阅通道接收消息
     *
     * @param channelKey 通道key
     * @param clazz      消息类型
     * @param consumer   自定义处理
     */
    public static <T> void subscribe(String channelKey, Class<T> clazz, Consumer<T> consumer) {
// 创建消息监听器适配器
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter((MessageListener) (message, pattern) -> {
            try {
                // 消息体反序列化
                T mesg = (T) redisTemplate.getValueSerializer().deserialize(message.getBody());
                if (mesg != null) {
                    consumer.accept(mesg);
                }
            }catch (Exception e) {
                log.error("消息处理异常 - channel: {}", channelKey, e);
            }
        });

        // 消息监听容器
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        container.addMessageListener(listenerAdapter, new ChannelTopic(channelKey));
        container.afterPropertiesSet();
        container.start();

        // 保存容器引用，以便后续可以取消订阅
        LISTENER_CONTAINERS.put(channelKey + ":" + consumer.hashCode(), container);
    }

    /**
     * 取消订阅
     *
     * @param channelKey 通道key
     * @param consumer   自定义处理
     */
    @SneakyThrows
    public static void unsubscribe(String channelKey, Consumer<?> consumer) {
        String key = channelKey + ":" + consumer.hashCode();
        RedisMessageListenerContainer container = LISTENER_CONTAINERS.remove(key);
        if (container != null) {
            container.stop();
            container.destroy();
        }
    }

    /**
     * 取消所有订阅
     */
    @SneakyThrows
    public static void unsubscribeAll() {
        for (RedisMessageListenerContainer container : LISTENER_CONTAINERS.values()) {
            container.stop();
            container.destroy();
        }
        LISTENER_CONTAINERS.clear();
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public static <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，保留当前对象 TTL 有效期
     *
     * @param key       缓存的键值
     * @param value     缓存的值
     * @param isSaveTtl 是否保留TTL有效期
     */
    public static <T> void setCacheObject(final String key, final T value, final boolean isSaveTtl) {
        if (isSaveTtl) {
            Long ttl = redisTemplate.getExpire(key);
            if (ttl != null && ttl > 0) {
                redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 缓存基本的对象，设置过期时间
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param duration 过期时间
     */
    public static <T> void setCacheObject(final String key, final T value, final Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * 如果不存在则设置
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     * @return set成功或失败
     */
    public static <T> boolean setObjectIfAbsent(final String key, final T value, final Duration duration) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, duration);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 如果存在则设置
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     * @return set成功或失败
     */
    public static <T> boolean setObjectIfExists(final String key, final T value, final Duration duration) {
        Boolean result = redisTemplate.opsForValue().setIfPresent(key, value, duration);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间（秒）
     * @return true=设置成功；false=设置失败
     */
    public static boolean expire(final String key, final long timeout) {
        Boolean result = redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置有效时间
     *
     * @param key      Redis键
     * @param duration 超时时间
     * @return true=设置成功；false=设置失败
     */
    public static boolean expire(final String key, final Duration duration) {
        Boolean result = redisTemplate.expire(key, duration);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获得缓存的基本对象
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public static <T> T getCacheObject(final String key) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        Object value = operations.get(key);
        return (T) value;
    }

    /**
     * 获得key剩余存活时间
     *
     * @param key 缓存键值
     * @return 剩余存活时间（秒）
     */
    public static long getTimeToLive(final String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -2L;
    }

    /**
     * 删除单个对象
     *
     * @param key 缓存的键值
     * @return 是否删除成功
     */
    public static boolean deleteObject(final String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 删除集合对象
     *
     * @param keys 多个key
     */
    public static void deleteObject(final Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 检查缓存对象是否存在
     *
     * @param key 缓存的键值
     */
    public static boolean isExistsObject(final String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public static <T> long setCacheList(final String key, final List<T> dataList) {
        Long result = redisTemplate.opsForList().rightPushAll(key, dataList.toArray());
        return result != null ? result : 0L;
    }

    /**
     * 追加缓存List数据
     *
     * @param key  缓存的键值
     * @param data 待缓存的数据
     * @return 缓存后的列表长度
     */
    public static <T> long addCacheList(final String key, final T data) {
        Long result = redisTemplate.opsForList().rightPush(key, data);
        return result != null ? result : 0L;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public static <T> List<T> getCacheList(final String key) {
        ListOperations<String, Object> operations = redisTemplate.opsForList();
        Long size = operations.size(key);
        if (size == null || size == 0) {
            return new ArrayList<>();
        }
        List<Object> range = operations.range(key, 0, size - 1);
        List<T> result = new ArrayList<>();
        for (Object obj : range) {
            result.add((T) obj);
        }
        return result;
    }

    /**
     * 获得缓存的list对象(范围)
     *
     * @param key  缓存的键值
     * @param from 起始下标
     * @param to   截止下标
     * @return 缓存键值对应的数据
     */
    public static <T> List<T> getCacheListRange(final String key, long from, long to) {
        ListOperations<String, Object> operations = redisTemplate.opsForList();
        List<Object> range = operations.range(key, from, to);
        List<T> result = new ArrayList<>();
        for (Object obj : range) {
            result.add((T) obj);
        }
        return result;
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的数量
     */
    public static <T> long setCacheSet(final String key, final Set<T> dataSet) {
        Long result = redisTemplate.opsForSet().add(key, dataSet.toArray());
        return result != null ? result : 0L;
    }

    /**
     * 追加缓存Set数据
     *
     * @param key  缓存的键值
     * @param data 待缓存的数据
     * @return 是否添加成功
     */
    public static <T> boolean addCacheSet(final String key, final T data) {
        Long result = redisTemplate.opsForSet().add(key, data);
        return result != null && result > 0;
    }

    /**
     * 获得缓存的set
     *
     * @param key 缓存的key
     * @return set对象
     */
    public static <T> Set<T> getCacheSet(final String key) {
        SetOperations<String, Object> operations = redisTemplate.opsForSet();
        Set<Object> members = operations.members(key);
        Set<T> result = new HashSet<>();
        if (members != null) {
            for (Object obj : members) {
                result.add((T) obj);
            }
        }
        return result;
    }

    /**
     * 缓存Map
     *
     * @param key     缓存的键值
     * @param dataMap 缓存的数据
     */
    public static <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key 缓存的键值
     * @return map对象
     */
    public static <T> Map<String, T> getCacheMap(final String key) {
        HashOperations<String, String, Object> operations = redisTemplate.opsForHash();
        Map<String, Object> entries = operations.entries(key);
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            result.put(entry.getKey(), (T) entry.getValue());
        }
        return result;
    }

    /**
     * 获得缓存Map的key列表
     *
     * @param key 缓存的键值
     * @return key列表
     */
    public static Set<String> getCacheMapKeySet(final String key) {
        HashOperations<String, String, Object> operations = redisTemplate.opsForHash();
        return operations.keys(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public static <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public static <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, Object> operations = redisTemplate.opsForHash();
        Object value = operations.get(key, hKey);
        return (T) value;
    }

    /**
     * 删除Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 删除的数量
     */
    public static long delCacheMapValue(final String key, final String hKey) {
        Long result = redisTemplate.opsForHash().delete(key, hKey);
        return result != null ? result : 0L;
    }

    /**
     * 删除Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return 删除的数量
     */
    public static long delMultiCacheMapValue(final String key, final Collection<String> hKeys) {
        Long result = redisTemplate.opsForHash().delete(key, hKeys.toArray());
        return result != null ? result : 0L;
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public static <T> List<T> getMultiCacheMapValue(final String key, final Collection<String> hKeys) {
        HashOperations<String, String, Object> operations = redisTemplate.opsForHash();
        List<Object> multiGet = operations.multiGet(key, hKeys);
        List<T> result = new ArrayList<>();
        for (Object obj : multiGet) {
            result.add((T) obj);
        }
        return result;
    }

    /**
     * 设置原子值
     *
     * @param key   Redis键
     * @param value 值
     */
    public static void setAtomicValue(String key, long value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 获取原子值
     *
     * @param key Redis键
     * @return 当前值
     */
    public static long getAtomicValue(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * 递增原子值
     *
     * @param key Redis键
     * @return 当前值
     */
    public static long incrAtomicValue(String key) {
        Long result = redisTemplate.opsForValue().increment(key);
        return result != null ? result : 0L;
    }

    /**
     * 递减原子值
     *
     * @param key Redis键
     * @return 当前值
     */
    public static long decrAtomicValue(String key) {
        Long result = redisTemplate.opsForValue().decrement(key);
        return result != null ? result : 0L;
    }

    /**
     * 获得缓存的基本对象列表(模式匹配)
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public static Set<String> keys(final String pattern) {
        return redisTemplate.keys(pattern + "*");
    }

    /**
     * 删除缓存的基本对象列表(模式匹配)
     *
     * @param pattern 字符串前缀
     */
    public static void deleteKeys(final String pattern) {
        Set<String> keys = redisTemplate.keys(pattern + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 检查redis中是否存在key
     *
     * @param key 键
     */
    public static Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

}