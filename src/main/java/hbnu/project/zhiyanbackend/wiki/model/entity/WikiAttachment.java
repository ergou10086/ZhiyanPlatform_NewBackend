package hbnu.project.zhiyanbackend.wiki.model.entity;


import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.wiki.model.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Wiki附件实体类（PostgreSQL）
 * 存储Wiki页面的附件元数据，包括图片和普通文件
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Getter
@Setter
@Entity
@Table(name = "wiki_attachment", indexes = {
        @Index(name = "idx_wiki_page", columnList = "wiki_page_id"),
        @Index(name = "idx_project", columnList = "project_id"),
        @Index(name = "idx_type", columnList = "attachment_type"),
        @Index(name = "idx_upload_by", columnList = "upload_by"),
        @Index(name = "idx_upload_at", columnList = "upload_at"),
        @Index(name = "idx_deleted", columnList = "is_deleted")
})
@DynamicInsert
@DynamicUpdate
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WikiAttachment {

    /**
     * 附件唯一标识（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 所属Wiki页面ID
     */
    @LongToString
    @Column(name = "wiki_page_id", nullable = false)
    private Long wikiPageId;

    /**
     * 所属项目ID（冗余字段，便于查询）
     */
    @LongToString
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 附件类型（IMAGE=图片, FILE=普通文件）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 20)
    private AttachmentType attachmentType;

    /**
     * 原始文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 文件类型/扩展名（jpg/png/pdf/zip等）
     */
    @Column(name = "file_type", length = 50)
    private String fileType;

    /**
     * MIME类型（image/jpeg, application/pdf等）
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * COS桶名
     */
    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    /**
     * COS对象键（存储路径）
     */
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    /**
     * 完整访问URL
     */
    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    /**
     * 文件描述/备注
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 文件MD5哈希值（用于去重和完整性校验）
     */
    @Column(name = "file_hash", length = 32)
    private String fileHash;

    /**
     * 上传者ID
     */
    @LongToString
    @Column(name = "upload_by", nullable = false)
    private Long uploadBy;

    /**
     * 上传时间
     */
    @Column(name = "upload_at", nullable = false)
    private LocalDateTime uploadAt;

    /**
     * 是否已删除（软删除标记）
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 删除者ID
     */
    @LongToString
    @Column(name = "deleted_by")
    private Long deletedBy;

    /**
     * 引用计数（被多少个Wiki页面引用）
     * 用于判断是否可以物理删除
     */
    @Builder.Default
    @Column(name = "reference_count")
    private Integer referenceCount = 0;

    /**
     * 额外元数据（使用JSONB存储扩展信息）
     * 例如：图片的宽高、视频的时长、文档的页数等
     */
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 缩略图URL（仅图片类型）
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /**
     * 在持久化之前生成雪花ID和上传时间
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        if (this.uploadAt == null) {
            this.uploadAt = LocalDateTime.now();
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.referenceCount == null) {
            this.referenceCount = 0;
        }
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
    }
}
