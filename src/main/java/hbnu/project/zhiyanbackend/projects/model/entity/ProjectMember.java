package hbnu.project.zhiyanbackend.projects.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtil;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 项目成员关系实体（精简版）
 * 对应表：project_members
 */
@Entity
@Table(name = "project_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_project_user", columnNames = {"project_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_project", columnList = "project_id"),
                @Index(name = "idx_user", columnList = "user_id")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {

    /**
     * 记录ID（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false,
            columnDefinition = "BIGINT")
    private Long id;

    /**
     * 项目ID
     */
    @LongToString
    @Column(name = "project_id", nullable = false,
            columnDefinition = "BIGINT")
    private Long projectId;

    /**
     * 用户ID
     */
    @LongToString
    @Column(name = "user_id", nullable = false,
            columnDefinition = "BIGINT")
    private Long userId;

    /**
     * 项目内角色
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false,
            columnDefinition = "VARCHAR(32)")
    private ProjectMemberRole projectRole;

    /**
     * 加入项目时间
     */
    @Column(name = "joined_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = SnowflakeIdUtil.nextId();
        }
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}

