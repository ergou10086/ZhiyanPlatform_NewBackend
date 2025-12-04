package hbnu.project.zhiyanbackend.activelog.model.entity;


import hbnu.project.zhiyanbackend.activelog.model.enums.AchievementOperationType;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 成果操作日志实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "achievement_operation_log", schema = "zhiyanactivelog",
        indexes = {
            @Index(name = "idx_project_id", columnList = "project_id"),
            @Index(name = "idx_achievement_id", columnList = "achievement_id"),
            @Index(name = "idx_user_id", columnList = "user_id"),
            @Index(name = "idx_operation_type", columnList = "operation_type"),
            @Index(name = "idx_operation_time", columnList = "operation_time"),
            @Index(name = "idx_project_achievement_time", columnList = "project_id, achievement_id, operation_time"),
            @Index(name = "idx_project_user_time", columnList = "project_id, user_id, operation_time")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementOperationLog {

    /**
     * 日志id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 项目id
     */
    @LongToString
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 成果id
     */
    @LongToString
    @Column(name = "achievement_id")
    private Long achievementId;

    /**
     * 成果标题
     * 冗余字段，加速
     */
    @Column(name = "achievement_title", length = 500)
    private String achievementTitle;

    /**
     * 操作的用户id
     */
    @LongToString
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 操作的用户名
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * 操作类型枚举
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private AchievementOperationType operationType;

    /**
     * 操作日志的对应业务模块
     */
    @Column(name = "operation_module", nullable = false, length = 50)
    private String operationModule = "成果管理";

    /**
     * 操作描述
     * 详细说明操作内容
     */
    @Column(name = "operation_desc", length = 500)
    private String operationDesc;

    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;


    /**
     * 在持久化之前生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        if (this.operationTime == null) {
            this.operationTime = LocalDateTime.now();
        }
    }
}
