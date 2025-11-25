package hbnu.project.zhiyanbackend.tasks.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.tasks.model.enums.AssignType;
import hbnu.project.zhiyanbackend.tasks.model.enums.RoleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * 任务用户关联实体
 */
@Entity
@Table(
        name = "task_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_task_user_role", columnNames = {"task_id", "user_id", "role_type"})
        },
        indexes = {
                @Index(name = "idx_task_id", columnList = "task_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_user_project", columnList = "user_id, project_id"),
                @Index(name = "idx_project_id", columnList = "project_id"),
                @Index(name = "idx_active", columnList = "is_active"),
                @Index(name = "idx_task_user_active", columnList = "task_id, user_id, is_active"),
                @Index(name = "idx_assigned_at", columnList = "assigned_at"),
                @Index(name = "idx_user_active", columnList = "user_id, is_active, assigned_at")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUser extends BaseAuditEntity {

    /**
     * 关联记录ID（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    private Long id;

    /**
     * 任务ID
     */
    @LongToString
    @Column(name = "task_id", nullable = false, columnDefinition = "BIGINT")
    private Long taskId;

    /**
     * 项目ID（冗余字段，提高查询性能）
     */
    @LongToString
    @Column(name = "project_id", nullable = false, columnDefinition = "BIGINT")
    private Long projectId;

    /**
     * 用户ID（执行者ID）
     */
    @LongToString
    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    private Long userId;

    /**
     * 分配类型：ASSIGNED-被管理员分配，CLAIMED-用户主动接取
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assign_type", nullable = false, columnDefinition = "VARCHAR(32)")
    private AssignType assignType;

    /**
     * 分配人ID（如果是CLAIMED则为user_id本身）
     */
    @LongToString
    @Column(name = "assigned_by", nullable = false, columnDefinition = "BIGINT")
    private Long assignedBy;

    /**
     * 分配/接取时间
     */
    @Column(name = "assigned_at")
    private Instant assignedAt;

    /**
     * 是否有效（TRUE-有效执行者，FALSE-已移除）
     */
    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN")
    private Boolean isActive;

    /**
     * 移除时间（仅当is_active=FALSE时有值）
     */
    @Column(name = "removed_at")
    private Instant removedAt;

    /**
     * 移除操作人ID（仅当is_active=FALSE时有值）
     */
    @LongToString
    @Column(name = "removed_by", columnDefinition = "BIGINT")
    private Long removedBy;

    /**
     * 角色类型：EXECUTOR-执行者，FOLLOWER-关注者，REVIEWER-审核者
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, columnDefinition = "VARCHAR(32)")
    private RoleType roleType;

    /**
     * 备注信息（可记录分配原因等）
     */
    @Column(name = "notes", length = 500, columnDefinition = "VARCHAR(500)")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        if (this.assignedAt == null) {
            this.assignedAt = Instant.now();
        }
        if (this.isActive == null) {
            this.isActive = Boolean.TRUE;
        }
        if (this.roleType == null) {
            this.roleType = RoleType.EXECUTOR;
        }
    }
}
