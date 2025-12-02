package hbnu.project.zhiyanbackend.tasks.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 任务提交记录实体类
 * 用于记录任务的提交、审核、退回历史
 *
 * @author Tokito
 */
@Entity
@Table(name = "task_submission", schema = "zhiyantasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmission {

    /**
     * 提交记录ID
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
     * 提交人ID（执行者ID）
     */
    @LongToString
    @Column(name = "submitter_id", nullable = false, columnDefinition = "BIGINT")
    private Long submitterId;


    /**
     * 提交说明（必填，描述完成情况）
     */
    @Column(name = "submission_content", nullable = false, columnDefinition = "TEXT")
    private String submissionContent;

    /**
     * 附件URL列表（可选，JSON数组格式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachment_urls", columnDefinition = "jsonb")
    private String attachmentUrls;

    /**
     * 提交时间
     */
    @Column(name = "submission_time")
    private Instant submissionTime;

    /**
     * 审核状态：PENDING-待审核，APPROVED-已批准，REJECTED-已拒绝，REVOKED-已撤回
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, columnDefinition = "VARCHAR(32)")
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    /**
     * 审核人ID（任务创建者）
     */
    @LongToString
    @Column(name = "reviewer_id", columnDefinition = "BIGINT")
    private Long reviewerId;

    /**
     * 审核意见（审核人填写）
     */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /**
     * 审核时间
     */
    @Column(name = "review_time")
    private Instant reviewTime;

    /**
     * 实际工时（单位：小时，提交时填写）
     */
    @Column(name = "actual_worktime", precision = 10, scale = 2)
    private BigDecimal actualWorktime;

    /**
     * 提交版本号（同一任务可以多次提交，版本号递增）
     */
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * 记录创建时间
     */
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * 记录更新时间
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * 是否已删除（软删除标记）
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.submissionTime == null) {
            this.submissionTime = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
