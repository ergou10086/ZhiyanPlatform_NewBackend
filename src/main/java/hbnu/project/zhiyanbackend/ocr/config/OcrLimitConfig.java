package hbnu.project.zhiyanbackend.ocr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OCR使用限制配置
 *
 * @author ErgouTree
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ocr.limit")
public class OcrLimitConfig {

    /**
     * 是否启用使用限制
     */
    private boolean enabled = true;

    /**
     * 默认每日限制次数（如果具体类型未配置则使用此值）
     */
    private Integer defaultDailyLimit = 5;
}
