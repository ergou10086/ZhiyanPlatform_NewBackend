package hbnu.project.zhiyanbackend.auth.config;

import cn.hutool.extra.mail.MailAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 邮件账户配置类
 * 将 Spring Boot 的邮件配置转换为 Hutool 的 MailAccount Bean
 *
 * @author ErgouTree
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class MailAccountConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port:465}")
    private Integer port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.default-encoding:UTF-8}")
    private String defaultEncoding;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private Boolean auth;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private Boolean sslEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.required:true}")
    private Boolean sslRequired;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.class:javax.net.ssl.SSLSocketFactory}")
    private String socketFactoryClass;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.port:465}")
    private Integer socketFactoryPort;

    /**
     * 创建 MailAccount Bean
     * 从 Spring Boot 的邮件配置中读取并转换为 Hutool 的 MailAccount
     */
    @Bean
    public MailAccount mailAccount() {
        MailAccount account = new MailAccount();
        
        // 基本配置
        account.setHost(host);
        account.setPort(port);
        account.setUser(username);
        account.setPass(password);
        account.setFrom(username);    // 默认使用用户名作为发件人
        account.setAuth(auth);
        account.setSslEnable(sslEnable);
        
        // 设置编码
        try {
            Charset charset = Charset.forName(defaultEncoding);
            account.setCharset(charset);
        } catch (Exception e) {
            log.warn("无法解析编码 {}，使用默认编码 UTF-8", defaultEncoding);
            account.setCharset(StandardCharsets.UTF_8);
        }
        
        // 设置 SMTP 属性
        Properties props = account.getSmtpProps();
        if (props == null) {
            props = new Properties();
        }
        props.setProperty("mail.smtp.auth", String.valueOf(auth));
        props.setProperty("mail.smtp.ssl.enable", String.valueOf(sslEnable));
        props.setProperty("mail.smtp.ssl.required", String.valueOf(sslRequired));
        props.setProperty("mail.smtp.socketFactory.class", socketFactoryClass);
        props.setProperty("mail.smtp.socketFactory.port", String.valueOf(socketFactoryPort));
        
        log.info("MailAccount Bean 初始化成功: host={}, port={}, username={}", 
                host, port, username);
        
        return account;
    }
}

