package hbnu.project.zhiyanbackend.ai.aiassistant.service;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.request.TaskResultGenerateRequest;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.TaskResultGenerateResponse;

import java.util.List;

/**
 * 任务结果AI生成服务接口
 * 该接口定义了与AI生成任务结果相关的核心功能方法
 */
public interface TaskResultAIGenerateService {

    /**
     * 生成任务结果草稿
     * @param request 任务结果生成请求参数
     * @return 返回生成的任务结果草稿内容
     */
    String generateTaskResultDraft(TaskResultGenerateRequest request);

    /**
     * 获取任务生成状态
     * @param jobId 任务唯一标识符
     * @param userId 用户ID
     * @return 返回任务生成状态响应信息
     */
    TaskResultGenerateResponse getGenerateStatus(String jobId, Long userId);

    /**
     * 取消任务生成
     * @param jobId 任务唯一标识符
     * @param userId 用户ID
     */
    void cancelGenerate(String jobId, Long userId);

    /**
     * 获取用户的所有AI草稿
     * @param userId 用户ID
     * @return 返回用户的所有AI生成草稿列表
     */
    List<TaskResultGenerateResponse> getAIDrafts(Long userId);
}
