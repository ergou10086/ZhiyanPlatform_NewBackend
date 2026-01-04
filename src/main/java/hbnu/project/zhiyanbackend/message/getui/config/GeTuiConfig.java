package hbnu.project.zhiyanbackend.message.getui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 个推消息推送配置类
 *
 * @author ErgouTree
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "getui")
public class GeTuiConfig {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * AppKey
     */
    private String appKey;

    /**
     * AppSercet
     */
    private String appSecret;

    /**
     * MasterSecret
     */
    private String masterSecret;

    /**
     * uni-app pack报名
     */
    private String appPackName = "uni.app.UNI43C3508";

    /**
     * 接口前缀
     */
    private String baseUrl = "https://restapi.getui.com/v2";

    /**
     * 获取完整的BaseUrl
     * 来自个推接口调用规范：https://docs.getui.com/getui/server/rest_v2/standard/
     */
    public String getFullBaseUrl() {
        return baseUrl + "/" + appId;
    }
}
