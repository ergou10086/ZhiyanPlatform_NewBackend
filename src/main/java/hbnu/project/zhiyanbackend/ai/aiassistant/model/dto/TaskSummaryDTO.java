package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 任务摘要数据传输对象(TaskSummaryDTO)
 * 用于在系统各层之间传递任务相关的核心信息
 * 使用Lombok注解简化代码，提供getter/setter、构建器模式和空参构造方法
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryDTO {

    private String id; // 任务唯一标识符

    private String projectId; // 所属项目ID

    private String projectName; // 项目名称

    private String title; // 任务标题

    private String description; // 任务描述

    private String status; // 任务状态（如：待办、进行中、已完成等）

    private String priority; // 任务优先级（如：低、中、高）

    private LocalDate dueDate; // 任务截止日期 // 创建者用户ID

    private String creatorId;
}
