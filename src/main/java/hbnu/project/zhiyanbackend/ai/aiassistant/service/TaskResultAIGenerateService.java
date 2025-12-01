package hbnu.project.zhiyanbackend.ai.aiassistant.service;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.request.TaskResultGenerateRequest;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.TaskResultGenerateResponse;

import java.util.List;

public interface TaskResultAIGenerateService {

    String generateTaskResultDraft(TaskResultGenerateRequest request);

    TaskResultGenerateResponse getGenerateStatus(String jobId, Long userId);

    void cancelGenerate(String jobId, Long userId);

    List<TaskResultGenerateResponse> getAIDrafts(Long userId);
}
