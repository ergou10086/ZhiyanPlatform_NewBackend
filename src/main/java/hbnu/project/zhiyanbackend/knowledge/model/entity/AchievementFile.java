package hbnu.project.zhiyanbackend.knowledge.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;

import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 文件管理表实体类
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Entity
@Table(name = "achievement_file", schema = "zhiyanknowledge", indexes = {
        @Index(name = "idx_achievement", columnList = "achievement_id"),
})
@DynamicInsert
@DynamicUpdate
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementFile extends BaseAuditEntity {

    /**
     * 文件唯一标识
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    private Long id;

    /**
     * 所属成果ID
     */
    @LongToString
    @Column(name = "achievement_id", nullable = false, columnDefinition = "BIGINT")
    private Long achievementId;

    /**
     * 原始文件名
     */
    @Column(name = "file_name", nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", columnDefinition = "BIGINT")
    private Long fileSize;

    /**
     * 文件类型（pdf/zip/csv等）
     * 通过读取文件后缀名自动添加
     */
    @Column(name = "file_type", length = 50, columnDefinition = "VARCHAR(50)")
    private String fileType;

    /**
     * 存储桶名称
     * 统一存储桶名称（单桶策略，通过目录区分业务）
     */
    @Column(name = "bucket_name", nullable = false, length = 100, columnDefinition = "VARCHAR(100) COMMENT '存储桶名称（兼容MinIO和COS）'")
    private String bucketName;

    /**
     * 对象存储键（Object Key）
     * 腾讯云COS，对象键（包含业务目录路径，如 achievement_file/2024/01/01/xxx.pdf）
     */
    @Column(name = "object_key", nullable = false, length = 500, columnDefinition = "VARCHAR(500) COMMENT '对象存储键（兼容MinIO和COS）'")
    private String objectKey;

    /**
     * 文件访问URL
     * 腾讯云COS: COS公网访问URL（通过 publicDomain + objectKey 构建）
     * 注意：此URL为完整访问地址，可直接用于文件下载或预览
     */
    @Column(name = "minio_url", nullable = false, length = 1000, columnDefinition = "VARCHAR(1000) COMMENT '文件访问URL（兼容MinIO和COS）'")
    private String cosUrl;

    /**
     * 上传者ID
     */
    @LongToString
    @Column(name = "upload_by", nullable = false, columnDefinition = "BIGINT")
    private Long uploadBy;

    /**
     * 上传时间
     */
    @Column(name = "upload_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime uploadAt;

    /**
     * 关联的成果实体（外键关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "achievement_file_ibfk_1"))
    private Achievement achievement;

    /**
     * 在持久化之前设置ID和上传时间
     */
    @PrePersist
    public void prePersist() {
        // 生成雪花ID
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        // 设置上传时间
        if (this.uploadAt == null) {
            this.uploadAt = LocalDateTime.now();
        }
    }
}
