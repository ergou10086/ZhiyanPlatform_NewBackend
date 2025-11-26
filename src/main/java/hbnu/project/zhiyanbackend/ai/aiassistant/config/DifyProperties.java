package hbnu.project.zhiyanbackend.ai.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    private String apiUrl;

    private String apiKey;

    private Integer timeout = 60000;

    private Integer maxTokens = 4096;

    private Double temperature = 0.7;

    private Boolean streamEnabled = true;
}
