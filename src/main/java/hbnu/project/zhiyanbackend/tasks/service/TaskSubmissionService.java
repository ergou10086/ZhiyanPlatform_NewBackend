package hbnu.project.zhiyanbackend.tasks.service;

import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import hbnu.project.zhiyanbackend.tasks.model.form.ReviewSubmissionRequest;
import hbnu.project.zhiyanbackend.tasks.model.form.SubmitTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 任务提交服务接口
 *
 * @author Tokito
 */
public interface TaskSubmissionService {

    TaskSubmissionDTO submitTask(Long taskId, SubmitTaskRequest request, Long userId);

    TaskSubmissionDTO reviewSubmission(Long submissionId, ReviewSubmissionRequest request, Long reviewerId);

    TaskSubmissionDTO revokeSubmission(Long submissionId, Long userId);

    TaskSubmissionDTO getSubmissionDetail(Long submissionId);

    List<TaskSubmissionDTO> getTaskSubmissions(Long taskId);

    TaskSubmissionDTO getLatestSubmission(Long taskId);

    Page<TaskSubmissionDTO> getPendingSubmissions(Long userId, Pageable pageable);

    Page<TaskSubmissionDTO> getProjectPendingSubmissions(Long projectId, Pageable pageable);

    Page<TaskSubmissionDTO> getUserSubmissions(Long userId, Pageable pageable);

    long countPendingSubmissions(Long userId);

    long countProjectPendingSubmissions(Long projectId);

    Page<TaskSubmissionDTO> getMyCreatedTasksPendingSubmissions(Long userId, Pageable pageable);

    long countMyCreatedTasksPendingSubmissions(Long userId);

    Page<TaskSubmissionDTO> getMyPendingSubmissions(Long userId, Pageable pageable);

    Page<TaskSubmissionDTO> getPendingSubmissionsForReview(Long userId, Pageable pageable);

    long countMyPendingSubmissions(Long userId);

    long countPendingSubmissionsForReview(Long userId);

    /**
     * 获取某个审核人审核过的提交记录（分页）
     * @param reviewerId 审核人ID
     * @param pageable 分页参数
     * @return 分页后的提交记录 DTO
     */
    Page<TaskSubmissionDTO> getSubmissionsByReviewer(Long reviewerId, Pageable pageable);

    Map<String, Object> getTaskSubmissionStats(Long taskId);

    /**
     * 查询我创建的任务中已审核通过的提交记录（分页）
     * @param userId 当前登录用户ID（任务创建者）
     * @param pageable 分页参数
     * @return 分页后的提交记录 DTO
     */
    Page<TaskSubmissionDTO> getReviewedSubmissionsForMyCreatedTasks(Long userId, Pageable pageable);

    /**
     * 批量查询任务的附件列表
     * @param taskIds 任务ID列表
     * @return Map<任务ID(字符串), 附件URL列表>，附件列表已去重
     */
    Map<String, List<String>> getTasksAttachments(List<Long> taskIds);
}
