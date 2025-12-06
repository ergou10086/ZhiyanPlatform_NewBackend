package hbnu.project.zhiyanbackend.security.encrypt.config;

import hbnu.project.zhiyanbackend.security.encrypt.config.properties.ApiDecryptProperties;
import hbnu.project.zhiyanbackend.security.encrypt.config.properties.EncryptorProperties;
import hbnu.project.zhiyanbackend.security.encrypt.core.EncryptorManager;
import hbnu.project.zhiyanbackend.security.encrypt.filter.CryptoFilter;
import hbnu.project.zhiyanbackend.security.encrypt.utils.FieldEncryptUtils;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * api 解密自动配置
 * 支持全量加密和字段级别加密
 *
 * @author wdhcr
 * @rewrite ErgouTree
 * @version 3.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties({ApiDecryptProperties.class, EncryptorProperties.class})
@ConditionalOnProperty(value = "api-decrypt.enabled", havingValue = "true")
public class ApiDecryptAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CryptoFilter> cryptoFilterRegistration(
            ApiDecryptProperties properties,
            EncryptorManager encryptorManager,
            EncryptorProperties encryptorProperties,
            FieldEncryptUtils fieldEncryptUtils) {
        FilterRegistrationBean<CryptoFilter> registration = new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new CryptoFilter(properties, encryptorManager, encryptorProperties, fieldEncryptUtils));
        registration.addUrlPatterns("/*");
        registration.setName("cryptoFilter");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        return registration;
    }
}
