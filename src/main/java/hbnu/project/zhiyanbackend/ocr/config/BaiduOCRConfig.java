package hbnu.project.zhiyanbackend.ocr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度OCR配置类
 *
 * @author ErgouTree
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baidu.ocr")
public class BaiduOCRConfig {

    /**
     * APPID
     */
    private String appId;

    /**
     * API Key
     * 百度云应用的AK
     */
    private String apiKey;

    /**
     * Sercet Key
     * 百度云的Sercet Key
     */
    private String secretKey;

    /**
     * Access Token URL
     */
    private String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";

    /**
     * 通用文字识别URL
     */
    private String generalBasicUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
}
