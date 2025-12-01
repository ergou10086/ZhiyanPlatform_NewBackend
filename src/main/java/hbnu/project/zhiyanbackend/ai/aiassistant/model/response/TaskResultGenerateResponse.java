package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultGenerateResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String jobId;

    private String status;

    private Map<String, Object> draftContent;

    private String errorMessage;

    @Builder.Default
    private Integer progress = 0;

    private Long userId;

    private Long projectId;

    /**
     * 本次生成涉及的任务ID列表
     * 用于后续将 AI 生成成果落库为任务成果并建立任务关联
     */
    private List<Long> taskIds;

    /**
     * 生成成果的标题（来自请求），用于创建知识库成果时复用
     */
    private String achievementTitle;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
