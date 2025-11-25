package hbnu.project.zhiyanbackend.message.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 消息收件人实体 - 收件人维度的消息记录
 * 记录每个收件人对消息的阅读状态
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "message_recipient", indexes = {
        @Index(name = "idx_receiver_status", columnList = "receiver_id, read_flag, deleted"),
        @Index(name = "idx_message_body", columnList = "message_body_id"),
        @Index(name = "idx_receiver_scene", columnList = "receiver_id, scene_code"),
        @Index(name = "idx_receiver_unread", columnList = "receiver_id, read_flag, trigger_time"),
        @Index(name = "idx_trigger_time", columnList = "trigger_time"),
        @Index(name = "idx_deleted", columnList = "deleted")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRecipient {

    /**
     * 收件记录ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @LongToString
    @Column(nullable = false, columnDefinition = "BIGINT")
    private Long id;

    /**
     * 关联消息体ID
     */
    @LongToString
    @Column(name = "message_body_id", nullable = false,
            columnDefinition = "BIGINT")
    private Long messageBodyId;

    /**
     * 接收人ID
     */
    @LongToString
    @Column(name = "receiver_id", nullable = false,
            columnDefinition = "BIGINT")
    private Long receiverId;

    /**
     * 场景代码（冗余字段，便于查询）
     */
    @Column(name = "scene_code", length = 50, columnDefinition = "VARCHAR(50)")
    private String sceneCode;

    /**
     * 是否已读
     */
    @Builder.Default
    @Column(name = "read_flag", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean readFlag = Boolean.FALSE;

    /**
     * 读取时间
     */
    @Column(name = "read_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime readAt;

    /**
     * 消息触发时间（冗余字段，便于排序）
     */
    @Column(name = "trigger_time", nullable = false, columnDefinition = "TIMESTAMP NOT NULL")
    private LocalDateTime triggerTime;

    /**
     * 是否已删除（软删除）
     */
    @Builder.Default
    @Column(name = "deleted", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean deleted = Boolean.FALSE;

    /**
     * 删除时间
     */
    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime deletedAt;

    /**
     * 关联消息体（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_body_id", insertable = false, updatable = false)
    private MessageBody messageBody;

    @PrePersist
    public void initMessageBodyId() {
        // 如果 messageBodyId 为 null，尝试从关联的 messageBody 获取
        if (this.messageBodyId == null && this.messageBody != null && this.messageBody.getId() != null) {
            this.messageBodyId = this.messageBody.getId();
        }
    }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.readFlag = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 软删除
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
