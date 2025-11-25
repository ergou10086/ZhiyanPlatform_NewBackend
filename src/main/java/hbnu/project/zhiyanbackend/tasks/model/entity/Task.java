package hbnu.project.zhiyanbackend.tasks.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskPriority;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 任务实体
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Task extends BaseAuditEntity {

    /**
     * 任务ID（雪花ID）
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
     * 任务创建者ID
     */
    @LongToString
    @Column(name = "creator_id", nullable = false, columnDefinition = "BIGINT")
    private Long creatorId;

    /**
     * 任务标题
     */
    @Column(name = "title", nullable = false, length = 200, columnDefinition = "VARCHAR(200)")
    private String title;

    /**
     * 任务描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 预估工时（单位：小时）
     */
    @Column(name = "worktime", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal worktime;

    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(32)")
    private TaskStatus status;

    /**
     * 任务优先级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, columnDefinition = "VARCHAR(16)")
    private TaskPriority priority;

    /**
     * 任务截止日期
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * 任务需要人数，默认单人任务
     */
    @Column(name = "required_people", nullable = false, columnDefinition = "INT")
    private Integer requiredPeople;

    /**
     * 是否已删除（软删除标记）
     */
    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN")
    private Boolean isDeleted;

    /**
     * 是否为里程碑任务
     */
    @Column(name = "is_milestone", nullable = false, columnDefinition = "BOOLEAN")
    private Boolean isMilestone;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        if (this.status == null) {
            this.status = TaskStatus.TODO;
        }
        if (this.priority == null) {
            this.priority = TaskPriority.MEDIUM;
        }
        if (this.requiredPeople == null) {
            this.requiredPeople = 1;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.isMilestone == null) {
            this.isMilestone = false;
        }
    }
}
