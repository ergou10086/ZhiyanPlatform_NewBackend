package hbnu.project.zhiyanbackend.message.unipush.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * unipush2.0消息推送配置类
 *
 * @author ErgouTree
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "unipush")
public class UnipushConfig {

    /**
     * DCloud AppID
     */
    private String appId;

    /**
     * DCloud AppKey
     */
    private String appKey;

    /**
     * DCloud MasterSecret
     */
    private String appSecret;

    /**
     * 推送消息的DCloud云url函数
     */
    private String pushUrl = "https://env-00jxugmemh8a.dev-hz.cloudbasefunction.cn/unipush";
}
