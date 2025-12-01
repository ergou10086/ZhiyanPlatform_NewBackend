package hbnu.project.zhiyanbackend.tasks.model.dto;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskPriority;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 任务详情DTO，包含执行者信息
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailDTO {

    /**
     * 任务ID
     */
    @LongToString
    private Long id;

    /**
     * 所属项目ID
     */
    @LongToString
    private Long projectId;

    /**
     * 任务创建者ID
     */
    @LongToString
    private Long creatorId;

    /**
     * 任务创建者名称
     */
    private String creatorName;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 预估工时（单位：小时）
     */
    private BigDecimal worktime;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 任务优先级
     */
    private TaskPriority priority;

    /**
     * 任务截止日期
     */
    private LocalDate dueDate;

    /**
     * 任务需要人数
     */
    private Integer requiredPeople;

    /**
     * 是否已删除
     */
    private Boolean isDeleted;

    /**
     * 是否为里程碑任务
     */
    private Boolean isMilestone;

    /**
     * 执行者列表
     */
    private List<AssigneeDTO> assignees;

    /**
     * 执行者信息DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssigneeDTO {
        @LongToString
        private Long userId;
        private String userName;
        private String email;
        private String avatarUrl;
    }
}
