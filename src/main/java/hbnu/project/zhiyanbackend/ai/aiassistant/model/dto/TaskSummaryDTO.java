package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryDTO {

    private String id;

    private String projectId;

    private String projectName;

    private String title;

    private String description;

    private String status;

    private String priority;

    private LocalDate dueDate;

    private String creatorId;
}
