package hbnu.project.zhiyanbackend.activelog.model.entity;

import hbnu.project.zhiyanbackend.activelog.model.enums.TaskOperationType;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 任务操作日志实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "task_operation_log", schema = "zhiyanactivelog",
        indexes = {
                @Index(columnList = "project_id"),
                @Index(columnList = "task_id"),
                @Index(columnList = "user_id"),
                @Index(columnList = "operation_type"),
                @Index(columnList = "operation_time"),
                @Index(columnList = "project_id, task_id, operation_time"),
                @Index(columnList = "project_id, user_id, operation_time")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaskOperationLog {

    /**
     * 日志ID（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    private Long id;

    /**
     * 项目ID
     */
    @LongToString
    @Column(name = "project_id", nullable = false, columnDefinition = "BIGINT NOT NULL")
    private Long projectId;

    /**
     * 任务ID（可为空，如批量操作）
     */
    @LongToString
    @Column(name = "task_id", columnDefinition = "BIGINT")
    private Long taskId;

    /**
     * 任务标题（冗余字段，便于查询）
     */
    @Column(name = "task_title", length = 500, columnDefinition = "VARCHAR(500)")
    private String taskTitle;

    /**
     * 操作用户ID
     */
    @LongToString
    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT NOT NULL")
    private Long userId;

    /**
     * 用户名（冗余字段，便于查询）
     */
    @Column(name = "username", length = 100, columnDefinition = "VARCHAR(100)")
    private String username;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL")
    private TaskOperationType operationType;

    /**
     * 操作模块
     */
    @Column(name = "operation_module", nullable = false, length = 50, columnDefinition = "VARCHAR(50) NOT NULL DEFAULT '任务管理'")
    private String operationModule = "任务管理";

    /**
     * 操作描述
     */
    @Column(name = "operation_desc", length = 500, columnDefinition = "VARCHAR(500)")
    private String operationDesc;

    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false, columnDefinition = "TIMESTAMPTZ NOT NULL")
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
