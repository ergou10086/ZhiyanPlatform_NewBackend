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

    Map<String, Object> getTaskSubmissionStats(Long taskId);
}
