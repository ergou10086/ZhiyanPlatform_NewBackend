package hbnu.project.zhiyanbackend.activelog.model.entity;

import hbnu.project.zhiyanbackend.activelog.model.enums.ProjectOperationType;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 项目操作日志实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "project_operation_log", schema = "zhiyanactivelog"
        , indexes = {
            @Index(name = "idx_project_operation_log_project_id", columnList = "project_id"),
            @Index(name = "idx_project_operation_log_user_id", columnList = "user_id"),
            @Index(name = "idx_project_operation_log_operation_type", columnList = "operation_type"),
            @Index(name = "idx_project_operation_log_operation_time", columnList = "operation_time"),
            @Index(name = "idx_project_operation_log_project_user_time", columnList = "project_id, user_id, operation_time"),
            @Index(name = "idx_project_operation_log_project_time", columnList = "project_id, operation_time")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectOperationLog {

    /**
     * 日志ID（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT PRIMARY KEY COMMENT '日志ID（雪花ID）'")
    private Long id;

    /**
     * 项目ID
     */
    @LongToString
    @Column(name = "project_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '项目ID'")
    private Long projectId;

    /**
     * 项目名称（冗余字段，便于查询）
     */
    @Column(name = "project_name", length = 200, columnDefinition = "VARCHAR(200) COMMENT '项目名称（冗余字段，便于查询）'")
    private String projectName;

    /**
     * 操作用户ID
     */
    @LongToString
    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '操作用户ID'")
    private Long userId;

    /**
     * 用户名（冗余字段，便于查询）
     */
    @Column(name = "username", length = 100, columnDefinition = "VARCHAR(100) COMMENT '用户名（冗余字段，便于查询）'")
    private String username;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL COMMENT '操作类型'")
    private ProjectOperationType operationType;

    /**
     * 操作模块
     */
    @Column(name = "operation_module", nullable = false, length = 50, columnDefinition = "VARCHAR(50) NOT NULL DEFAULT '项目管理' COMMENT '操作模块'")
    private String operationModule = "项目管理";

    /**
     * 操作描述
     */
    @Column(name = "operation_desc", length = 500, columnDefinition = "VARCHAR(500) COMMENT '操作描述'")
    private String operationDesc;

    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false, columnDefinition = "TIMESTAMPTZ NOT NULL COMMENT '操作时间'")
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
