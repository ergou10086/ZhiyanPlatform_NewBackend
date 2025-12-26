package hbnu.project.zhiyanbackend.message.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 邮件通知偏好设置DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationPreferenceDTO {

    /**
     * 是否启用邮件通知（总开关）
     */
    private Boolean enabled;

    /**
     * 启用的业务场景列表
     */
    private Set<String> enabledScenes;
}

