package hbnu.project.zhiyanbackend.basic.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 使用 Caffeine 作为本地缓存实现
 *
 * 缓存策略：
 * 1. achievementTasks: 缓存成果关联的任务列表（热点数据）
 * 2. achievementList: 缓存项目成果列表（读多写少）
 * 3. taskUserMap: 缓存任务负责人映射（高频查询）
 *
 * @author ErgouTree
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     * 使用 Caffeine 作为缓存实现
     *
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "achievementTasks",    // 成果关联任务缓存
                "achievementList",     // 成果列表缓存
                "taskUserMap"          // 任务负责人缓存
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());

        return cacheManager;
    }

    /**
     * 配置 Caffeine 缓存构建器
     *
     * 配置说明：
     * - initialCapacity: 初始容量 100
     * - maximumSize: 最大缓存 1000 条
     * - expireAfterWrite: 写入后 10 分钟过期
     * - expireAfterAccess: 访问后 5 分钟过期
     * - recordStats: 记录缓存统计信息
     *
     * @return Caffeine 构建器
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats();
    }
}