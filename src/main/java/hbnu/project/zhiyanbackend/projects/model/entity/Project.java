package hbnu.project.zhiyanbackend.projects.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 项目实体（精简版）
 * 对应表：projects
 *
 * @author Tokito
 */
@Entity
@Table(name = "projects", schema = "zhiyanproject")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Project extends BaseAuditEntity {

    /**
     * 项目ID（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false,
            columnDefinition = "BIGINT")
    private Long id;

    /**
     * 项目名称
     */
    @Column(name = "name", nullable = false, length = 200,
            columnDefinition = "VARCHAR(200)")
    private String name;

    /**
     * 项目描述
     */
    @Column(name = "description",
            columnDefinition = "TEXT")
    private String description;

    /**
     * 项目状态
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProjectStatus status = ProjectStatus.PLANNING;

    /**
     * 项目可见性
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    private ProjectVisibility visibility = ProjectVisibility.PRIVATE;

    /**
     * 开始日期
     */
    @Column(name = "start_date",
            columnDefinition = "DATE")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @Column(name = "end_date",
            columnDefinition = "DATE")
    private LocalDate endDate;

    /**
     * 项目封面图片二进制数据
     */
    @Column(name = "image_data", columnDefinition = "BYTEA")
    private byte[] imageData;

    /**
     * 项目封面图片在对象存储中的对象键
     */
    @Column(name = "image_object_key", length = 512)
    private String imageObjectKey;

    /**
     * 项目封面图片的公网访问地址
     */
    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    /**
     * 创建人 ID（逻辑关联用户表）
     */
    @LongToString
    @Column(name = "creator_id", nullable = false,
            columnDefinition = "BIGINT")
    private Long creatorId;

    /**
     * 软删除标记
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false,
            columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted = false;

    /**
     * 持久化前自动生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
    }
}

