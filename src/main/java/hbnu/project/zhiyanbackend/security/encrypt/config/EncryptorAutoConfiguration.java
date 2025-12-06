package hbnu.project.zhiyanbackend.security.encrypt.config;

import hbnu.project.zhiyanbackend.security.encrypt.config.properties.EncryptorProperties;
import hbnu.project.zhiyanbackend.security.encrypt.core.EncryptContext;
import hbnu.project.zhiyanbackend.security.encrypt.core.EncryptorManager;
import hbnu.project.zhiyanbackend.security.encrypt.utils.EncryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 加密器自动配置类
 * 自动配置请求/响应字段加密功能
 *
 * @author ErgouTree
 * @version 3.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(EncryptorProperties.class)
@ConditionalOnProperty(prefix = "zhiyan.encrypt", name = "enabled", havingValue = "true")
public class EncryptorAutoConfiguration {

    /**
     * 创建加密上下文
     */
    @Bean
    public EncryptContext encryptContext(EncryptorProperties properties) {
        EncryptContext context = EncryptContext.from(properties);

        // 将配置同步到 EncryptUtils
        EncryptUtils.setDefaultAesKey(properties.getAesKey());
        EncryptUtils.setDefaultSm4Key(properties.getSm4Key());
        if (properties.getRsaPublicKey() != null && properties.getRsaPrivateKey() != null) {
            EncryptUtils.setDefaultRsaKeys(properties.getRsaPublicKey(), properties.getRsaPrivateKey());
        }

        log.info("加密上下文初始化完成 - 默认算法: {}", properties.getAlgorithm());
        return context;
    }

    /**
     * 创建加密器管理器
     * 用于管理加密器实例和缓存加密字段信息
     */
    @Bean
    public EncryptorManager encryptorManager() {
        EncryptorManager manager = new EncryptorManager();
        log.info("加密器管理器初始化完成 - 支持字段级别加密");
        return manager;
    }
}
