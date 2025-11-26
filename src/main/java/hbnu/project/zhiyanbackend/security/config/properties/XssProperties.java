package hbnu.project.zhiyanbackend.security.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS跨站脚本配置
 *
 * @author ErgouTree
 */
@Data
@ConfigurationProperties(prefix = "security.xss")
public class XssProperties {

    /**
     * 是否启用XSS防护
     */
    private boolean enabled = true;

    /**
     * 排除的URL路径（支持Ant风格通配符）
     */
    private List<String> excludeUrls = new ArrayList<>();
}
