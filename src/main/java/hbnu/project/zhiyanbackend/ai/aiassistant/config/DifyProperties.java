package hbnu.project.zhiyanbackend.ai.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DifyProperties类
 * 用于配置Dify相关属性的配置类
 * 通过@ConfigurationProperties注解将配置文件中以dify为前缀的属性自动绑定到该类的字段上
 *
 * @author Tokito
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    /**
     * API接口URL
     * 用于指定Dify服务的访问地址
     */
    private String apiUrl;

    /**
     * API密钥
     * 用于访问Dify服务的认证凭据
     */
    private String apiKey;

    /**
     * 请求超时时间，单位为毫秒
     * 默认值为60000毫秒（60秒）
     */
    private Integer timeout = 60000;

    /**
     * 最大令牌数
     * 用于限制单次请求的最大处理长度
     * 默认值为4096
     */
    private Integer maxTokens = 4096;

    /**
     * 温度参数
     * 控制生成内容的随机性和创造性
     * 取值范围通常在0到1之间，默认值为0.7
     */
    private Double temperature = 0.7;

    /**
     * 是否启用流式输出
     * 默认为true，表示启用流式输出模式
     */
    private Boolean streamEnabled = true;
}
