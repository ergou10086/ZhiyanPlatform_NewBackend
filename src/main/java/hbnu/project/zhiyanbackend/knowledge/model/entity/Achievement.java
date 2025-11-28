package hbnu.project.zhiyanbackend.knowledge.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.knowledge.model.converter.AchievementTypeConverter;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementType;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

/**
 * 成果主表实体类
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Entity
@Table(name = "achievement", schema = "zhiyanknowledge", indexes = {
        @Index(name = "idx_project_status", columnList = "project_id, status"),
        @Index(name = "idx_creator", columnList = "creator_id"),
        @Index(name = "idx_type", columnList = "type")
})
// 插入时忽略null值
@DynamicInsert
// 更新时只更新修改的字段
@DynamicUpdate
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement extends BaseAuditEntity {

    /**
     * 成果唯一标识（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    private Long id;

    /**
     * 所属项目ID
     */
    @LongToString
    @Column(name = "project_id", nullable = false, columnDefinition = "BIGINT")
    private Long projectId;

    /**
     * 成果类型：paper(论文)、patent(专利)、dataset(数据集)、model(模型)、report(报告)、custom(自定义)、task_result(任务成果)
     */
    @Convert(converter = AchievementTypeConverter.class)
    @Column(name = "type", nullable = false, length = 50, columnDefinition = "VARCHAR(50) CHECK (type IN ('paper', 'patent', 'dataset', 'model', 'report', 'custom', 'task_result'))")
    private AchievementType type;

    /**
     * 成果标题
     */
    @Column(name = "title", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String title;

    /**
     * 成果的公开性，默认不公开
     */
    @Column(name = "is_public", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * 创建者ID
     */
    @LongToString
    @Column(name = "creator_id", nullable = false, columnDefinition = "BIGINT")
    private Long creatorId;

    /**
     * 状态：draft(草稿)、under_review(审核中)、published(已发布)、obsolete（过时）
     * 这个状态先简单调整，用户发布就是已发布，然后由用户手动调整状态
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'draft' CHECK (status IN ('draft', 'under_review', 'published', 'obsolete'))")
    private AchievementStatus status = AchievementStatus.draft;

    /**
     * 关联成果详情表（一对一）
     */
    @OneToOne(mappedBy = "achievement", cascade = CascadeType.ALL, orphanRemoval = true)
    private AchievementDetail detail;

    /**
     * 关联文件列表（一对多）
     */
    @OneToMany(mappedBy = "achievement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AchievementFile> files;

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
