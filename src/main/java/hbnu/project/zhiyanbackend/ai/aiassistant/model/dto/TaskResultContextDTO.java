package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务结果上下文数据传输对象(DTO)
 * 用于封装任务相关的结果信息，包括任务摘要、提交记录、最新提交和最终批准的提交
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultContextDTO {

    // 任务摘要信息，包含任务的基本情况
    private TaskSummaryDTO task;

    // 任务提交记录列表，包含该任务的所有提交历史
    private List<TaskSubmissionDTO> submissions;

    // 任务的最新提交记录，可能是最新一次的提交
    private TaskSubmissionDTO latestSubmission;

    // 任务最终被批准的提交记录，可能是审核通过的最终版本
    private TaskSubmissionDTO finalApprovedSubmission;
}
