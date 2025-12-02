package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultContextDTO {

    private TaskSummaryDTO task;

    private List<TaskSubmissionDTO> submissions;

    private TaskSubmissionDTO latestSubmission;

    private TaskSubmissionDTO finalApprovedSubmission;
}
