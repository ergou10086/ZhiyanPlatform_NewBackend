package hbnu.project.zhiyanbackend.wiki.model.entity;

import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wiki页面实体类（PostgreSQL）
 * 存储元数据、当前内容和最近版本历史
 * 支持树状目录结构：可以是目录(DIRECTORY)或文档(DOCUMENT)
 *
 * @author ErgouTree
 * @rewrite ErgouTree,yui,conversation
 */
@Getter
@Setter
@Entity
@Table(name = "wiki_page", schema = "zhiyanwiki",
        indexes = {
                @Index(name = "idx_project", columnList = "project_id"),
                @Index(name = "idx_parent", columnList = "parent_id"),
                @Index(name = "idx_project_parent", columnList = "project_id, parent_id"),
                @Index(name = "idx_project_type", columnList = "project_id, page_type"),
                @Index(name = "idx_path", columnList = "path"),
                @Index(name = "idx_updated_at", columnList = "updated_at")
        })
@DynamicInsert
@DynamicUpdate
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WikiPage extends BaseAuditEntity {

    /**
     * Wiki页面唯一标识（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * wiki页面所属项目ID
     */
    @LongToString
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 页面标题
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * 页面类型（目录 DIRECTORY 或文档 DOCUMENT）
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", nullable = false, length = 20)
    private PageType pageType = PageType.DOCUMENT;

    /**
     * 父页面ID（用于构建树状结构，根页面为null）
     */
    @LongToString
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 页面路径（用于快速查找，如 "/root/parent/current"）
     * 使用ltree类型可以更高效地进行层级查询
     */
    @Column(name = "path", length = 1000)
    private String path;

    /**
     * 排序序号（用于同级页面排序）
     */
    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 是否公开（默认只对项目成员可见）
     */
    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = false;

    /**
     * 创建者ID
     * 复用父类的createdBy
     */
    @Override
    @LongToString
    @Column(name = "creator_id", nullable = false)
    public Long getCreatedBy() {
        return super.getCreatedBy();
    }

    /**
     * 最后编辑者ID
     * 复用父类的updatedBy
     */
    @Override
    @LongToString
    @Column(name = "last_editor_id")
    public Long getUpdatedBy() {
        return super.getUpdatedBy();
    }

    // ==================== 内容相关字段 ====================

    /**
     * Markdown 内容（当前最新版本）
     * 使用TEXT类型存储大文本
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 内容哈希值（用于检测内容是否真正变化）
     * 使用SHA-256哈希
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * 内容大小（字符数，用于快速统计）
     */
    @Column(name = "content_size")
    private Integer contentSize;

    /**
     * 当前版本号
     */
    @Builder.Default
    @Column(name = "current_version")
    private Integer currentVersion = 1;

    /**
     * 内容摘要（前200字符，用于列表展示）
     */
    @Column(name = "content_summary", length = 200)
    private String contentSummary;

    /**
     * 全文搜索向量（PostgreSQL tsvector类型）
     * 用于高性能全文搜索
     */
    @Column(name = "search_vector", columnDefinition = "tsvector")
    private String searchVector;

    // ==================== 版本历史（JSONB存储最近10个版本） ====================

    /**
     * 最近版本历史（保留最近 10 个版本）
     * 使用PostgreSQL的JSONB类型存储，支持索引和查询
     */
    @Type(JsonBinaryType.class)
    @Column(name = "recent_versions", columnDefinition = "jsonb")
    @Builder.Default
    private List<RecentVersionInfo> recentVersions = new ArrayList<>();

    // ==================== 协同编辑相关字段（预留） ====================

    /**
     * 是否被锁定（协同编辑时，锁定状态防止并发冲突）
     * TODO: 暂未使用，待协同编辑功能实现
     */
    @Builder.Default
    @Column(name = "is_locked")
    private Boolean isLocked = false;

    /**
     * 锁定者用户ID（正在编辑的用户）
     * TODO: 暂未使用，待协同编辑功能实现
     */
    @LongToString
    @Column(name = "locked_by")
    private Long lockedBy;

    /**
     * 锁定时间
     * TODO: 暂未使用，待协同编辑功能实现
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /**
     * 是否启用协同编辑模式
     * TODO: 暂未使用，待协同编辑功能实现
     */
    @Builder.Default
    @Column(name = "collaborative_mode")
    private Boolean collaborativeMode = false;

    /**
     * 当前在线编辑者列表（使用PostgreSQL数组类型）
     * TODO: 暂未使用，待协同编辑功能实现
     */
    @Type(ListArrayType.class)
    @Column(name = "active_editors", columnDefinition = "bigint[]")
    @Builder.Default
    private List<Long> activeEditors = new ArrayList<>();

    /**
     * 操作序列号（用于协同编辑的操作变换 OT）
     * TODO: 暂未使用，待协同编辑功能实现
     */
    @Column(name = "operation_sequence")
    private Long operationSequence;

    /**
     * 最后一次协同编辑同步时间
     * TODO: 暂未使用，待协同编辑功能实现
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;


    /**
     * 在持久化之前生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        if (this.recentVersions == null) {
            this.recentVersions = new ArrayList<>();
        }
        if (this.activeEditors == null) {
            this.activeEditors = new ArrayList<>();
        }
    }


    /**
     * 最近版本信息（嵌套在JSONB中）
     * 保存差异补丁而非完整内容，节省存储空间
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentVersionInfo {
        /**
         * 版本号
         */
        private Integer version;

        /**
         * 相对于前一版本的差异补丁（Unified Diff 格式）
         */
        private String contentDiff;

        /**
         * 变更描述（用户提交的说明）
         */
        private String changeDescription;

        /**
         * 编辑者用户ID
         */
        private Long editorId;

        /**
         * 版本创建时间
         */
        private LocalDateTime createdAt;

        /**
         * 新增行数
         */
        private Integer addedLines;

        /**
         * 删除行数
         */
        private Integer deletedLines;

        /**
         * 变更字符数
         */
        private Integer changedChars;

        /**
         * 内容哈希值（该版本的完整内容哈希）
         */
        private String contentHash;
    }
}