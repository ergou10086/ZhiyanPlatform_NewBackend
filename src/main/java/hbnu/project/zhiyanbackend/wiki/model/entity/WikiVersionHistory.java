package hbnu.project.zhiyanbackend.wiki.model.entity;

import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * Wiki版本历史记录实体类（PostgreSQL）
 * 当wiki_page表中的recent_versions超过10个时，
 * 旧的版本会被归档到这个表中
 * 使用分区表按project_id进行分区，提高查询性能
 *
 * @author ErgouTree
 * @rewrite ErgouTree,yui,conversation
 */
@Getter
@Setter
@Entity
@Table(name = "wiki_version_history",schema = "zhiyanwiki", indexes = {
        @Index(name = "idx_wiki_version", columnList = "wiki_page_id, version"),
        @Index(name = "idx_project_created", columnList = "project_id, created_at"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@DynamicInsert
@DynamicUpdate
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 继承审计基类
public class WikiVersionHistory extends BaseAuditEntity {

    /**
     * 历史记录唯一标识（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 关联 wiki_page 表的 ID
     */
    @LongToString
    @Column(name = "wiki_page_id", nullable = false)
    private Long wikiPageId;

    /**
     * 冗余字段，方便查询和分区
     */
    @LongToString
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 版本号
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * 相对于前一版本的差异补丁（Unified Diff 格式）
     * 使用TEXT类型存储大文本
     */
    @Column(name = "content_diff", columnDefinition = "TEXT")
    private String contentDiff;

    /**
     * 变更描述
     */
    @Column(name = "change_description", length = 500)
    private String changeDescription;

    /**
     * 编辑者用户ID
     * 覆盖父类的createdBy，因为版本历史的编辑者即创建者
     * @modify conversation
     */
    @Override
    @LongToString
    @Column(name = "editor_id", nullable = false)
    public Long getCreatedBy() {
        return super.getCreatedBy();
    }

    /**
     * 版本创建时间
     * 覆盖父类的createdAt
     * @modify conversation
     */
    @Override
    @Column(name = "created_at", nullable = false)
    public LocalDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    /**
     * 新增行数
     */
    @Column(name = "added_lines")
    private Integer addedLines;

    /**
     * 删除行数
     */
    @Column(name = "deleted_lines")
    private Integer deletedLines;

    /**
     * 变更字符数
     */
    @Column(name = "changed_chars")
    private Integer changedChars;

    /**
     * 内容哈希值
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * 归档时间（当该版本从recent移入history时的时间）
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * 在持久化之前生成雪花ID和归档时间
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        if (this.archivedAt == null) {
            this.archivedAt = LocalDateTime.now();
        }
    }
}