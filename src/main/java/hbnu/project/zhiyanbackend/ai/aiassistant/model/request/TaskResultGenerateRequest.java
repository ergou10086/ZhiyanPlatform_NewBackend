package hbnu.project.zhiyanbackend.ai.aiassistant.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultGenerateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;

    private String achievementTitle;

    @Builder.Default
    private List<Long> taskIds = new ArrayList<>();

    private String targetAudience;

    private String additionalRequirements;

    @Builder.Default
    private Boolean includeAttachments = false;

    @Builder.Default
    private List<String> attachmentFilters = new ArrayList<>();

    private Long userId;
}
