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
@Table(name = "message_send_record", schema = "zhiyanmessage")
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
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 消息体ID
     */
    @LongToString
    @Column(name = "message_body_id", nullable = false)
    private Long messageBodyId;

    /**
     * 发送时间
     */
    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime;

    /**
     * 目标收件人总数
     */
    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients;

    /**
     * 成功发送数量
     */
    @Builder.Default
    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    /**
     * 失败数量
     */
    @Builder.Default
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    /**
     * 发送状态
     */
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "SENDING";

    /**
     * 数据创建时间
     */
    @JsonIgnore
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 数据最后修改时间
     */
    @JsonIgnore
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * 数据创建人ID
     */
    @LongToString
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 数据最后修改人ID
     */
    @LongToString
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 版本号（乐观锁）
     */
    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    /**
     * 关联消息体
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_body_id", insertable = false, updatable = false)
    private MessageBody messageBody;

}
