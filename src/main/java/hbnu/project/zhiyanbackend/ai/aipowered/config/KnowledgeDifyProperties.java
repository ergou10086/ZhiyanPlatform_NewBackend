package hbnu.project.zhiyanbackend.ai.aipowered.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识库Dify配置属性类
 * 用于映射配置文件中以"dify.knowledge"为前缀的配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify.knowledge")
public class KnowledgeDifyProperties {

    /**
     * Dify API的URL地址
     */
    private String apiUrl;

    /**
     * Dify API的密钥
     */
    private String apiKey;

    /**
     * 请求超时时间，默认为60000毫秒
     */
    private Integer timeout = 60000;

    /**
     * 最大令牌数，默认为4096
     */
    private Integer maxTokens = 4096;

    /**
     * 温度参数，控制输出的随机性，默认为0.7
     */
    private Double temperature = 0.7;

    /**
     * 是否启用流式输出，默认为true
     */
    private Boolean streamEnabled = true;
}
