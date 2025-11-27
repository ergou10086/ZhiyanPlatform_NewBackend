package hbnu.project.zhiyanbackend.auth.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 用户学术成果关联实体
 * 用于记录用户手动关联的学术成果（论文、专利等）
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "user_achievements", schema = "zhiyanauth",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_USER_ACHIEVEMENT",
                columnNames = {"user_id", "achievement_id"}),
        indexes = {
                @Index(name = "idx_user_achievements_user_id", columnList = "user_id"),
                @Index(name = "idx_user_achievements_achievement_id", columnList = "achievement_id"),
                @Index(name = "idx_user_achievements_project_id", columnList = "project_id")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievement extends BaseAuditEntity {

    /**
     * 关联记录ID（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户ID
     */
    @LongToString
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 成果ID（关联知识库模块的成果）
     */
    @LongToString
    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    /**
     * 所属项目ID
     */
    @LongToString
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 展示顺序（用户可自定义排序）
     */
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * 备注说明（用户对该成果的个人说明）
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 在持久化之前生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
    }
}

