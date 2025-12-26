package hbnu.project.zhiyanbackend.message.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户邮件通知偏好设置实体
 * 记录用户希望接收哪些业务场景的邮件通知
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "user_email_notification_preference", schema = "zhiyanmessage",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailNotificationPreference extends BaseAuditEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户ID
     */
    @LongToString
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 是否启用邮件通知（总开关）
     */
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    /**
     * 启用的业务场景列表（JSON数组格式）
     * 存储格式：["TASK_OVERDUE", "TASK_REVIEW_REQUEST", "TASK_REVIEW_RESULT", ...]
     * 只存储高优先级的业务场景
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_scenes", columnDefinition = "jsonb")
    private String enabledScenes;

    /**
     * 获取启用的场景集合（辅助方法）
     */
    public Set<String> getEnabledScenesSet() {
        if (enabledScenes == null || enabledScenes.trim().isEmpty()) {
            return new HashSet<>();
        }
        try {
            return new java.util.HashSet<>(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(enabledScenes, java.util.List.class)
            );
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    /**
     * 设置启用的场景集合（辅助方法）
     */
    public void setEnabledScenesSet(Set<String> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            this.enabledScenes = null;
        } else {
            try {
                this.enabledScenes = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(new java.util.ArrayList<>(scenes));
            } catch (Exception e) {
                this.enabledScenes = null;
            }
        }
    }

    /**
     * 检查某个场景是否启用
     */
    public boolean isSceneEnabled(String scene) {
        if (!enabled) {
            return false;
        }
        return getEnabledScenesSet().contains(scene);
    }
}

