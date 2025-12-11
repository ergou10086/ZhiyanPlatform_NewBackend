package hbnu.project.zhiyanbackend.tasks.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.message.service.MessageSendService;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskSubmission;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskUser;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import hbnu.project.zhiyanbackend.tasks.model.form.ReviewSubmissionRequest;
import hbnu.project.zhiyanbackend.tasks.model.form.SubmitTaskRequest;
import hbnu.project.zhiyanbackend.tasks.repository.TaskRepository;
import hbnu.project.zhiyanbackend.tasks.repository.TaskSubmissionRepository;
import hbnu.project.zhiyanbackend.tasks.repository.TaskUserRepository;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务提交服务实现类
 *
 * @author Tokito
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    private final TaskSubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final TaskUserRepository taskUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberService projectMemberService;
    private final ObjectMapper objectMapper;
    private final MessageSendService messageSendService;
    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final hbnu.project.zhiyanbackend.activelog.core.OperationLogHelper operationLogHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSubmissionDTO submitTask(Long taskId, SubmitTaskRequest request, Long userId) {
        log.info("用户[{}]提交任务[{}]", userId, taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new IllegalArgumentException("任务已删除，无法提交");
        }

        // 检查任务是否已逾期
        boolean isOverdue = false;
        if (task.getDueDate() != null) {
            LocalDate today = LocalDate.now();
            isOverdue = task.getDueDate().isBefore(today);
            log.info("检查任务逾期: taskId={}, dueDate={}, today={}, isOverdue={}",
                    taskId, task.getDueDate(), today, isOverdue);
            // 不再阻止逾期任务的提交，允许用户补交
            // 但会发送逾期通知
        }

        boolean isAssignee = taskUserRepository.isUserActiveExecutor(taskId, userId);
        if (!isAssignee) {
            throw new IllegalArgumentException("只有任务执行者才能提交任务");
        }

        if (task.getStatus() == TaskStatus.DONE) {
            throw new IllegalArgumentException("任务已完成，无法重复提交");
        }

        Integer nextVersion = submissionRepository.getNextVersionNumber(taskId);

        String attachmentUrlsJson = null;
        if (request.getAttachmentUrls() != null && !request.getAttachmentUrls().isEmpty()) {
            try {
                log.info("任务提交附件URL列表: taskId={}, attachmentUrls={}", taskId, request.getAttachmentUrls());
                attachmentUrlsJson = objectMapper.writeValueAsString(request.getAttachmentUrls());
                log.info("任务提交附件URL序列化结果: taskId={}, json={}", taskId, attachmentUrlsJson);
            } catch (JsonProcessingException e) {
                log.error("附件URL序列化失败", e);
                throw new IllegalArgumentException("附件URL格式错误");
            }
        }

        TaskSubmission submission = TaskSubmission.builder()
                .taskId(taskId)
                .projectId(task.getProjectId())
                .submitterId(userId)
                .submissionContent(request.getSubmissionContent())
                .attachmentUrls(attachmentUrlsJson)
                .actualWorktime(request.getActualWorktime())
                .version(nextVersion)
                .reviewStatus(ReviewStatus.PENDING)
                .isDeleted(false)
                .build();

        submission = submissionRepository.save(submission);
        log.info("任务提交成功: submissionId={}, version={}",
                submission.getId(), nextVersion);

        if (task.getStatus() != TaskStatus.DONE) {
            task.setStatus(TaskStatus.PENDING_REVIEW);
            taskRepository.save(task);
            log.info("任务状态已更新为待审核: taskId={}", taskId);

            // 发送待审核任务通知给任务创建者（审核者）
            messageSendService.notifyTaskReviewRequest(task, submission, userId);
        }

        // 如果任务已逾期，发送逾期通知给任务执行者
        long overdueDays = ChronoUnit.DAYS.between(task.getDueDate(), LocalDate.now());
        if (isOverdue) {
            try {
                List<TaskUser> executors = taskUserRepository.findActiveExecutorsByTaskId(taskId);
                for (TaskUser executor : executors) {
                    // 只给提交者之外的其他执行者发送逾期通知，因为提交者已经提交或者补交，无需提醒
                    if (!executor.getUserId().equals(userId)) {
                        messageSendService.notifyTaskOverSubmissionTime(task, executor.getUserId(), overdueDays);
                    }
                }
            } catch (Exception e) {
                log.error("发送任务逾期通知失败: taskId={}", taskId, e);
                // 通知发送失败不影响主流程
            }
        }

        // 记录提交任务操作日志
        try {
            operationLogHelper.logTaskSubmit(task.getProjectId(), taskId, task.getTitle());
        } catch (Exception e) {
            log.warn("记录提交任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }

        return convertToDTO(submission, task);
    }

    /**
     * 审核任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSubmissionDTO reviewSubmission(Long submissionId, ReviewSubmissionRequest request, Long reviewerId) {
        log.info("用户[{}]审核提交记录[{}]，结果: {}", reviewerId, submissionId, request.getReviewStatus());

        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));

        if (Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new IllegalArgumentException("提交记录已删除");
        }

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalArgumentException("该提交已审核，无法重复操作");
        }

        Task task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("关联任务不存在"));

        if (!reviewerId.equals(task.getCreatorId())) {
            throw new IllegalArgumentException("只有任务创建者才能审核提交");
        }

        if (request.getReviewStatus() != ReviewStatus.APPROVED
                && request.getReviewStatus() != ReviewStatus.REJECTED) {
            throw new IllegalArgumentException("审核结果只能是APPROVED或REJECTED");
        }

        submission.setReviewStatus(request.getReviewStatus());
        submission.setReviewerId(reviewerId);
        submission.setReviewComment(request.getReviewComment());
        submission.setReviewTime(Instant.now());

        submission = submissionRepository.save(submission);
        log.info("提交记录审核完成: submissionId={}, status={}", submissionId, request.getReviewStatus());

        // 根据审核结果更新任务状态
        if (request.getReviewStatus() == ReviewStatus.APPROVED
                && task.getStatus() != TaskStatus.DONE) {
            // 审核通过：更新任务状态为已完成
            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);
            log.info("任务已完成: taskId={}", task.getId());
        } else if (request.getReviewStatus() == ReviewStatus.REJECTED
                && task.getStatus() != TaskStatus.DONE) {
            // 审核拒绝：更新任务状态为进行中，让用户可以继续修改并重新提交
            // 注意：如果任务已经是DONE状态，不更新（避免已完成的任务被改回进行中）
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(task);
            log.info("任务审核被拒绝，状态已从 {} 更新为进行中: taskId={}", oldStatus, task.getId());
        }

        // 发送审核结果通知（审核通过或拒绝时发送）
        try {
            messageSendService.notifyTaskSubmissionReviewed(task, submission, request.getReviewStatus(), reviewerId);
        } catch (Exception e) {
            log.error("发送任务审核结果通知失败: submissionId={}, reviewStatus={}", submissionId, request.getReviewStatus(), e);
            // 通知发送失败不影响主流程
        }

        // 记录审核任务操作日志
        try {
            String reviewResult = request.getReviewStatus() == ReviewStatus.APPROVED ? "通过" : "拒绝";
            operationLogHelper.logTaskReview(task.getProjectId(), task.getId(), task.getTitle(), reviewResult);
            
            // 如果审核通过，任务完成，记录完成日志
            if (request.getReviewStatus() == ReviewStatus.APPROVED && task.getStatus() == TaskStatus.DONE) {
                operationLogHelper.logTaskComplete(task.getProjectId(), task.getId(), task.getTitle());
            }
        } catch (Exception e) {
            log.warn("记录审核任务日志失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
        }

        return convertToDTO(submission, task);
    }

    /**
     * 撤回审核
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSubmissionDTO revokeSubmission(Long submissionId, Long userId) {
        log.info("用户[{}]撤回提交记录[{}]", userId, submissionId);

        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));

        if (Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new IllegalArgumentException("提交记录已删除");
        }

        if (!submission.getSubmitterId().equals(userId)) {
            throw new IllegalArgumentException("只有提交人才能撤回提交");
        }

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalArgumentException("只能撤回待审核的提交");
        }

        submission.setReviewStatus(ReviewStatus.REVOKED);
        submission = submissionRepository.save(submission);
        log.info("提交记录撤回成功: submissionId={}", submissionId);

        Task task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("关联任务不存在"));

        return convertToDTO(submission, task);
    }

/**
 * 根据提交记录ID获取提交详情
 * 此方法为只读事务方法
 * @param submissionId 提交记录ID
 * @return TaskSubmissionDTO 提交详情的数据传输对象
 * @throws IllegalArgumentException 当提交记录不存在或已删除或关联任务不存在时抛出
 */
    @Override
    @Transactional(readOnly = true)  // 声明这是一个只读事务方法
    public TaskSubmissionDTO getSubmissionDetail(Long submissionId) {
    // 根据ID查找提交记录，如果不存在则抛出异常
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));

    // 检查提交记录是否已被删除
        if (Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new IllegalArgumentException("提交记录已删除");
        }

    // 根据提交记录中的任务ID查找关联任务，如果不存在则抛出异常
        Task task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("关联任务不存在"));

    // 将提交记录和任务信息转换为数据传输对象并返回
        return convertToDTO(submission, task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSubmissionDTO> getTaskSubmissions(Long taskId) {
        List<TaskSubmission> submissions = submissionRepository
                .findByTaskIdAndIsDeletedFalseOrderByVersionDesc(taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        return submissions.stream()
                .map(s -> convertToDTO(s, task))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskSubmissionDTO getLatestSubmission(Long taskId) {
        // 如果没有提交记录，返回null而不是抛出异常
        Optional<TaskSubmission> submissionOpt = submissionRepository
                .findFirstByTaskIdAndIsDeletedFalseOrderByVersionDesc(taskId);
        
        if (submissionOpt.isEmpty()) {
            return null;
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        return convertToDTO(submissionOpt.get(), task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getPendingSubmissions(Long userId, Pageable pageable) {
        return submissionRepository
                .findPendingSubmissionsForUser(userId, ReviewStatus.PENDING, pageable)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getProjectPendingSubmissions(Long projectId, Pageable pageable) {
        return submissionRepository
                .findByProjectIdAndReviewStatusAndIsDeletedFalseOrderBySubmissionTimeDesc(
                        projectId, ReviewStatus.PENDING, pageable)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getUserSubmissions(Long userId, Pageable pageable) {
        return submissionRepository
                .findBySubmitterIdAndIsDeletedFalseOrderBySubmissionTimeDesc(userId, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public long countPendingSubmissions(Long userId) {
        return submissionRepository.countPendingSubmissionsForUser(userId, ReviewStatus.PENDING);
    }

    @Override
    public long countProjectPendingSubmissions(Long projectId) {
        return submissionRepository.countByProjectIdAndReviewStatusAndIsDeletedFalse(
                projectId, ReviewStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getMyCreatedTasksPendingSubmissions(Long userId, Pageable pageable) {
        return submissionRepository
                .findPendingSubmissionsForMyCreatedTasks(userId, ReviewStatus.PENDING, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public long countMyCreatedTasksPendingSubmissions(Long userId) {
        return submissionRepository.countPendingSubmissionsForMyCreatedTasks(userId, ReviewStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getMyPendingSubmissions(Long userId, Pageable pageable) {
        return submissionRepository
                .findMyPendingSubmissions(userId, ReviewStatus.PENDING, pageable)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getPendingSubmissionsForReview(Long userId, Pageable pageable) {
        return submissionRepository
                .findPendingSubmissionsForReviewer(userId, ReviewStatus.PENDING, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public long countMyPendingSubmissions(Long userId) {
        return submissionRepository.countMyPendingSubmissions(userId, ReviewStatus.PENDING);
    }

    @Override
    public long countPendingSubmissionsForReview(Long userId) {
        return submissionRepository.countPendingSubmissionsForReviewer(userId, ReviewStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTaskSubmissionStats(Long taskId) {
        Map<String, Object> stats = new HashMap<>();

        List<TaskUser> executors = taskUserRepository.findActiveExecutorsByTaskId(taskId);
        stats.put("totalExecutors", executors.size());
        
        // 所有提交（按版本倒序）
        List<TaskSubmission> allSubmissions = submissionRepository
                .findByTaskIdAndIsDeletedFalseOrderByVersionDesc(taskId);
        stats.put("totalSubmissions", allSubmissions.size());

        // 审核通过的提交
        List<TaskSubmission> approvedSubmissions = allSubmissions.stream()
                .filter(s -> s.getReviewStatus() == ReviewStatus.APPROVED)
                .toList();
        stats.put("approvedSubmissions", approvedSubmissions.size());

        // 按执行者分组统计提交
        Map<Long, List<TaskSubmission>> submissionsByExecutor = allSubmissions.stream()
                .collect(Collectors.groupingBy(TaskSubmission::getSubmitterId));

        stats.put("submissionsByExecutor", submissionsByExecutor);
        stats.put("canBeMarkedAsDone", !approvedSubmissions.isEmpty());

        return stats;
    }

    private TaskSubmissionDTO convertToDTO(TaskSubmission submission, Task task) {
        List<String> attachmentUrls = new ArrayList<>();
        if (submission.getAttachmentUrls() != null) {
            try {
                attachmentUrls = objectMapper.readValue(
                        submission.getAttachmentUrls(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                log.warn("附件URL反序列化失败", e);
            }
        }

        String projectName = null;
        try {
            if (submission.getProjectId() != null) {
                Optional<Project> projectOpt = projectRepository.findById(submission.getProjectId());
                projectName = projectOpt.map(Project::getName).orElse(null);
            }
        } catch (Exception e) {
            log.warn("获取项目名称失败，projectId: {}", submission.getProjectId(), e);
        }

        // 查询提交人信息
        UserDTO submitter = null;
        try {
            if (submission.getSubmitterId() != null) {
                Optional<User> userOpt = userRepository.findById(submission.getSubmitterId());
                submitter = userOpt.map(userConverter::toDTO).orElse(null);
            }
        } catch (Exception e) {
            log.warn("获取提交人信息失败，submitterId: {}", submission.getSubmitterId(), e);
        }

        // 查询审核人信息
        UserDTO reviewer = null;
        try {
            if (submission.getReviewerId() != null) {
                Optional<User> userOpt = userRepository.findById(submission.getReviewerId());
                reviewer = userOpt.map(userConverter::toDTO).orElse(null);
            }
        } catch (Exception e) {
            log.warn("获取审核人信息失败，reviewerId: {}", submission.getReviewerId(), e);
        }

        return TaskSubmissionDTO.builder()
                .id(String.valueOf(submission.getId()))
                .taskId(String.valueOf(submission.getTaskId()))
                .taskTitle(task != null ? task.getTitle() : null)
                .taskCreatorId(task != null && task.getCreatorId() != null ? String.valueOf(task.getCreatorId()) : null)
                .projectId(String.valueOf(submission.getProjectId()))
                .projectName(projectName)
                .submitterId(String.valueOf(submission.getSubmitterId()))
                .submitter(submitter)
                .submissionContent(submission.getSubmissionContent())
                .attachmentUrls(attachmentUrls)
                .submissionTime(instantToLocalDateTime(submission.getSubmissionTime()))
                .reviewStatus(submission.getReviewStatus())
                .reviewerId(submission.getReviewerId() != null ? String.valueOf(submission.getReviewerId()) : null)
                .reviewer(reviewer)
                .reviewComment(submission.getReviewComment())
                .reviewTime(instantToLocalDateTime(submission.getReviewTime()))
                .actualWorktime(submission.getActualWorktime())
                .version(submission.getVersion())
                .createdAt(instantToLocalDateTime(submission.getCreatedAt()))
                .updatedAt(instantToLocalDateTime(submission.getUpdatedAt()))
                .build();
    }

    private TaskSubmissionDTO convertToDTO(TaskSubmission submission) {
        Task task = taskRepository.findById(submission.getTaskId()).orElse(null);
        return convertToDTO(submission, task);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getTasksAttachments(List<Long> taskIds) {
        log.info("批量查询任务附件: taskIds={}", taskIds);
        
        if (taskIds == null || taskIds.isEmpty()) {
            return new HashMap<>();
        }

        // 批量查询所有任务的提交记录
        List<TaskSubmission> submissions = submissionRepository
                .findByTaskIdInAndIsDeletedFalseOrderByTaskIdAscVersionDesc(taskIds);

        // 按任务ID分组，收集所有附件URL并去重
        Map<String, List<String>> result = new HashMap<>();
        
        for (TaskSubmission submission : submissions) {
            String taskIdStr = String.valueOf(submission.getTaskId());
            
            // 解析附件URL列表
            List<String> attachmentUrls = new ArrayList<>();
            if (submission.getAttachmentUrls() != null && !submission.getAttachmentUrls().trim().isEmpty()) {
                try {
                    attachmentUrls = objectMapper.readValue(
                            submission.getAttachmentUrls(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                } catch (JsonProcessingException e) {
                    log.warn("任务附件URL反序列化失败: taskId={}, submissionId={}", 
                            submission.getTaskId(), submission.getId(), e);
                }
            }
            
            // 如果该任务还没有在结果中，初始化列表
            result.putIfAbsent(taskIdStr, new ArrayList<>());
            
            // 添加附件URL（去重）
            List<String> existingUrls = result.get(taskIdStr);
            for (String url : attachmentUrls) {
                if (url != null && !url.trim().isEmpty() && !existingUrls.contains(url)) {
                    existingUrls.add(url);
                }
            }
        }
        
        // 确保所有请求的任务ID都在结果中（即使没有附件）
        for (Long taskId : taskIds) {
            String taskIdStr = String.valueOf(taskId);
            result.putIfAbsent(taskIdStr, new ArrayList<>());
        }
        
        log.info("批量查询任务附件完成: 查询了{}个任务，找到{}个有附件的任务", 
                taskIds.size(), result.values().stream().filter(list -> !list.isEmpty()).count());
        
        return result;
    }

    private LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
