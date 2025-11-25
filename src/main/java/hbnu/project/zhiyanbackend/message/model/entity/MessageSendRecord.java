package hbnu.project.zhiyanbackend.message.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;


/**
 * 消息发送记录实体（可选）
 * 用于记录群组消息和广播消息的发送情况
 * 如果需要追踪"消息发送给了哪些人"的统计信息，可以使用此表
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "message_send_record", indexes = {
        @Index(name = "idx_message_body", columnList = "message_body_id"),
        @Index(name = "idx_send_time", columnList = "send_time"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendRecord {

    /**
     * 记录ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @LongToString
    @Column(nullable = false, columnDefinition = "BIGINT")
    private Long id;

    /**
     * 消息体ID
     */
    @LongToString
    @Column(name = "message_body_id", nullable = false,
            columnDefinition = "BIGINT")
    private Long messageBodyId;

    /**
     * 发送时间
     */
    @Column(name = "send_time", nullable = false,
            columnDefinition = "TIMESTAMP NOT NULL")
    private LocalDateTime sendTime;

    /**
     * 目标收件人总数
     */
    @Column(name = "total_recipients", nullable = false,
            columnDefinition = "INTEGER NOT NULL")
    private Integer totalRecipients;

    /**
     * 成功发送数量
     */
    @Column(name = "success_count", nullable = false,
            columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    private Integer successCount;

    /**
     * 失败数量
     */
    @Column(name = "failed_count", nullable = false,
            columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    private Integer failedCount;

    /**
     * 发送状态
     */
    @Column(name = "status", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'SENDING'")
    private String status;

    /**
     * 数据创建时间
     */
    @JsonIgnore
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 数据最后修改时间
     */
    @JsonIgnore
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * 数据创建人ID
     */
    @LongToString
    @Column(name = "created_by", columnDefinition = "BIGINT")
    private Long createdBy;

    /**
     * 数据最后修改人ID
     */
    @LongToString
    @Column(name = "updated_by", columnDefinition = "BIGINT")
    private Long updatedBy;

    /**
     * 版本号（乐观锁）
     */
    @Version
    @Builder.Default
    @Column(name = "version", nullable = false,
            columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    private Integer version = 0;

    /**
     * 关联消息体
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_body_id", insertable = false, updatable = false)
    private MessageBody messageBody;

}
