package hbnu.project.zhiyanbackend.auth.config;

import cn.hutool.extra.mail.MailAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Hutool MailAccount 配置类
 * 用于解决 MailUtils 静态初始化时需要的 MailAccount Bean
 * 
 * @author ErgouTree
 */
@Slf4j
@Configuration
public class MailAccountConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private Integer port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private Boolean auth;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private Boolean sslEnable;

    @Value("${spring.mail.default-encoding:UTF-8}")
    private String charset;

    /**
     * 创建 Hutool MailAccount Bean
     * 从 Spring Boot 的邮件配置中读取参数
     */
    @Bean
    public MailAccount mailAccount() {
        MailAccount account = new MailAccount();
        
        // 基本配置
        account.setHost(host);
        account.setPort(port);
        account.setFrom(username);
        account.setUser(username);
        account.setPass(password);
        account.setAuth(auth);
        account.setSslEnable(sslEnable);
        
        // 如果启用了 SSL，需要设置相关属性
        if (sslEnable) {
            account.setStarttlsEnable(true);
            // 设置 Socket Factory
            account.setSocketFactoryClass("javax.net.ssl.SSLSocketFactory");
            account.setSocketFactoryPort(port);
        }
        
        log.info("Hutool MailAccount Bean 初始化成功 - Host: {}, Port: {}, User: {}", host, port, username);
        
        return account;
    }
}