package hbnu.project.zhiyanbackend.tasks.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.UserBasicInfo;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.message.service.MessageSendService;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskBasicInfo;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务提交服务实现类
 * <p>
 * 核心职责：处理任务提交、审核、撤回等核心业务逻辑，提供任务提交记录的查询、统计、计数等能力
 * 设计原则：
 * 1. 写操作（提交/审核/撤回）开启事务并支持回滚
 * 2. 读操作（查询/统计）设置只读事务提升性能
 * 3. 批量查询优先使用投影/基础信息查询，避免全字段查询
 *
 * @author Tokito
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    // ======================== 依赖注入区 ========================
    /** 任务提交记录仓储 */
    private final TaskSubmissionRepository submissionRepository;
    /** 任务仓储 */
    private final TaskRepository taskRepository;
    /** 任务-用户关联仓储 */
    private final TaskUserRepository taskUserRepository;
    /** 项目仓储 */
    private final ProjectRepository projectRepository;
    /** JSON序列化/反序列化工具 */
    private final ObjectMapper objectMapper;
    /** 消息发送服务（用于推送审核/逾期通知） */
    private final MessageSendService messageSendService;
    /** 用户仓储 */
    private final UserRepository userRepository;
    /** 用户实体-DTO转换器 */
    private final UserConverter userConverter;
    /** 操作日志记录助手 */
    private final hbnu.project.zhiyanbackend.activelog.core.OperationLogHelper operationLogHelper;

    // ======================== 核心写操作（事务型） ========================

    /**
     * 提交任务
     * <p>
     * 核心逻辑：
     * 1. 校验任务有效性（存在/未删除/未完成）
     * 2. 校验提交人身份（必须是任务执行者）
     * 3. 处理附件URL序列化
     * 4. 生成提交版本号，保存提交记录
     * 5. 更新任务状态为待审核，发送审核通知
     * 6. 逾期任务发送逾期通知（不阻止提交）
     *
     * @param taskId  任务ID
     * @param request 提交请求参数（内容/附件/实际工时）
     * @param userId  提交人ID
     * @return 提交记录DTO
     * @throws IllegalArgumentException 任务不存在/已删除/提交人无权限/任务已完成等异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSubmissionDTO submitTask(Long taskId, SubmitTaskRequest request, Long userId) {
        log.info("用户[{}]提交任务[{}]", userId, taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new IllegalArgumentException("任务已删除，无法提交");
        }

        // 检查任务是否已逾期（仅通知，不阻止提交）
        boolean isOverdue = false;
        if (task.getDueDate() != null) {
            LocalDate today = LocalDate.now();
            isOverdue = task.getDueDate().isBefore(today);
            log.info("检查任务逾期: taskId={}, dueDate={}, today={}, isOverdue={}",
                    taskId, task.getDueDate(), today, isOverdue);
        }

        // 校验提交人是否为任务执行者
        boolean isAssignee = taskUserRepository.isUserActiveExecutor(taskId, userId);
        if (!isAssignee) {
            throw new IllegalArgumentException("只有任务执行者才能提交任务");
        }

        // 校验任务是否已完成
        if (task.getStatus() == TaskStatus.DONE) {
            throw new IllegalArgumentException("任务已完成，无法重复提交");
        }

        // 获取下一个提交版本号
        Integer nextVersion = submissionRepository.getNextVersionNumber(taskId);

        // 序列化附件URL列表
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

        // 构建并保存提交记录
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
        log.info("任务提交成功: submissionId={}, version={}", submission.getId(), nextVersion);

        // 更新任务状态为待审核，并发送审核通知
        if (task.getStatus() != TaskStatus.DONE) {
            task.setStatus(TaskStatus.PENDING_REVIEW);
            taskRepository.save(task);
            log.info("任务状态已更新为待审核: taskId={}", taskId);
            messageSendService.notifyTaskReviewRequest(task, submission, userId);
        }

        // 逾期任务发送逾期通知（仅给非提交人执行者）
        long overdueDays = ChronoUnit.DAYS.between(task.getDueDate(), LocalDate.now());
        if (isOverdue) {
            try {
                List<TaskUser> executors = taskUserRepository.findActiveExecutorsByTaskId(taskId);
                for (TaskUser executor : executors) {
                    if (!executor.getUserId().equals(userId)) {
                        messageSendService.notifyTaskOverSubmissionTime(task, executor.getUserId(), overdueDays);
                    }
                }
            } catch (Exception e) {
                log.error("发送任务逾期通知失败: taskId={}", taskId, e);
            }
        }

        // 记录提交操作日志
        try {
            operationLogHelper.logTaskSubmit(task.getProjectId(), taskId, task.getTitle());
        } catch (Exception e) {
            log.warn("记录提交任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }

        return convertToDTO(submission, task);
    }

    /**
     * 审核任务提交记录
     * <p>
     * 核心逻辑：
     * 1. 校验提交记录有效性（存在/未删除/待审核）
     * 2. 校验审核人身份（必须是任务创建者）
     * 3. 更新审核状态/审核人/审核意见/审核时间
     * 4. 根据审核结果更新任务状态（通过→完成；拒绝→进行中+延长截止日期3天）
     * 5. 发送审核结果通知，记录操作日志
     *
     * @param submissionId 提交记录ID
     * @param request      审核请求参数（审核结果/审核意见）
     * @param reviewerId   审核人ID
     * @return 审核后的提交记录DTO
     * @throws IllegalArgumentException 提交记录不存在/已审核/审核人无权限等异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSubmissionDTO reviewSubmission(Long submissionId, ReviewSubmissionRequest request, Long reviewerId) {
        log.info("用户[{}]审核提交记录[{}]，结果: {}", reviewerId, submissionId, request.getReviewStatus());

        // 校验提交记录存在且未删除
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));
        if (Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new IllegalArgumentException("提交记录已删除");
        }

        // 校验提交记录处于待审核状态
        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalArgumentException("该提交已审核，无法重复操作");
        }

        // 校验关联任务存在，且审核人是任务创建者
        Task task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("关联任务不存在"));
        if (!reviewerId.equals(task.getCreatorId())) {
            throw new IllegalArgumentException("只有任务创建者才能审核提交");
        }

        // 校验审核结果合法性
        if (request.getReviewStatus() != ReviewStatus.APPROVED && request.getReviewStatus() != ReviewStatus.REJECTED) {
            throw new IllegalArgumentException("审核结果只能是APPROVED或REJECTED");
        }

        // 更新审核信息
        submission.setReviewStatus(request.getReviewStatus());
        submission.setReviewerId(reviewerId);
        submission.setReviewComment(request.getReviewComment());
        submission.setReviewTime(Instant.now());
        submission = submissionRepository.save(submission);
        log.info("提交记录审核完成: submissionId={}, status={}", submissionId, request.getReviewStatus());

        // 根据审核结果更新任务状态
        if (request.getReviewStatus() == ReviewStatus.APPROVED && task.getStatus() != TaskStatus.DONE) {
            // 审核通过：任务置为已完成
            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);
            log.info("任务已完成: taskId={}", task.getId());
        } else if (request.getReviewStatus() == ReviewStatus.REJECTED && task.getStatus() != TaskStatus.DONE) {
            // 审核拒绝：任务置为进行中，截止日期延长3天
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(TaskStatus.IN_PROGRESS);
            try {
                LocalDate newDue = LocalDate.now().plusDays(3);
                task.setDueDate(newDue);
                log.info("任务审核被拒绝，截止日期已延长至 {}: taskId={}, oldDue={}, newDue={}",
                        newDue, task.getId(), task.getDueDate(), newDue);
            } catch (Exception e) {
                log.warn("延长任务截止日期失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
            }
            taskRepository.save(task);
            log.info("任务审核被拒绝，状态已从 {} 更新为进行中: taskId={}", oldStatus, task.getId());
        }

        // 发送审核结果通知
        try {
            messageSendService.notifyTaskSubmissionReviewed(task, submission, request.getReviewStatus(), reviewerId);
        } catch (Exception e) {
            log.error("发送任务审核结果通知失败: submissionId={}, reviewStatus={}", submissionId, request.getReviewStatus(), e);
        }

        // 记录审核操作日志（审核通过额外记录完成日志）
        try {
            String reviewResult = request.getReviewStatus() == ReviewStatus.APPROVED ? "通过" : "拒绝";
            operationLogHelper.logTaskReview(task.getProjectId(), task.getId(), task.getTitle(), reviewResult);
            if (request.getReviewStatus() == ReviewStatus.APPROVED && task.getStatus() == TaskStatus.DONE) {
                operationLogHelper.logTaskComplete(task.getProjectId(), task.getId(), task.getTitle());
            }
        } catch (Exception e) {
            log.warn("记录审核任务日志失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
        }

        return convertToDTO(submission, task);
    }

    /**
     * 撤回待审核的任务提交记录
     * <p>
     * 核心逻辑：
     * 1. 校验提交记录有效性（存在/未删除）
     * 2. 校验撤回人身份（必须是提交人）
     * 3. 校验提交记录处于待审核状态
     * 4. 更新提交记录状态为已撤回
     *
     * @param submissionId 提交记录ID
     * @param userId       撤回人ID（提交人）
     * @return 撤回后的提交记录DTO
     * @throws IllegalArgumentException 提交记录不存在/已删除/撤回人无权限/非待审核状态等异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSubmissionDTO revokeSubmission(Long submissionId, Long userId) {
        log.info("用户[{}]撤回提交记录[{}]", userId, submissionId);

        // 校验提交记录存在且未删除
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));
        if (Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new IllegalArgumentException("提交记录已删除");
        }

        // 校验撤回人是提交人
        if (!submission.getSubmitterId().equals(userId)) {
            throw new IllegalArgumentException("只有提交人才能撤回提交");
        }

        // 校验提交记录处于待审核状态
        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalArgumentException("只能撤回待审核的提交");
        }

        // 更新撤回状态
        submission.setReviewStatus(ReviewStatus.REVOKED);
        submission = submissionRepository.save(submission);
        log.info("提交记录撤回成功: submissionId={}", submissionId);

        // 查询关联任务，转换DTO返回
        Task task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("关联任务不存在"));

        return convertToDTO(submission, task);
    }

    // ======================== 单条/批量读操作（只读事务） ========================

    /**
     * 根据提交记录ID获取提交详情
     *
     * @param submissionId 提交记录ID
     * @return 提交详情DTO
     * @throws IllegalArgumentException 提交记录不存在/已删除/关联任务不存在
     */
    @Override
    @Transactional(readOnly = true)
    public TaskSubmissionDTO getSubmissionDetail(Long submissionId) {
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));

        if (Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new IllegalArgumentException("提交记录已删除");
        }

        Task task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("关联任务不存在"));

        return convertToDTO(submission, task);
    }

    /**
     * 根据任务ID获取该任务的所有提交记录（按版本倒序）
     *
     * @param taskId 任务ID
     * @return 任务提交记录DTO列表
     * @throws IllegalArgumentException 任务不存在
     */
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

    /**
     * 获取任务的最新提交记录（最高版本）
     * <p>无提交记录时返回null，不抛出异常
     *
     * @param taskId 任务ID
     * @return 最新提交记录DTO（无则null）
     * @throws IllegalArgumentException 任务不存在
     */
    @Override
    @Transactional(readOnly = true)
    public TaskSubmissionDTO getLatestSubmission(Long taskId) {
        Optional<TaskSubmission> submissionOpt = submissionRepository
                .findFirstByTaskIdAndIsDeletedFalseOrderByVersionDesc(taskId);

        if (submissionOpt.isEmpty()) {
            return null;
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        return convertToDTO(submissionOpt.get(), task);
    }

    /**
     * 批量获取多个任务的提交记录（按任务ID分组）
     * <p>优化点：使用投影查询任务/用户基础信息，避免全字段查询
     *
     * @param taskIds 任务ID列表
     * @return 键：任务ID字符串；值：该任务的提交记录DTO列表
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<TaskSubmissionDTO>> batchGetTaskSubmissions(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 清洗任务ID（非空/正数/去重）
        List<Long> sanitized = taskIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (sanitized.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查询所有符合条件的提交记录
        List<TaskSubmission> submissions = submissionRepository
                .findByTaskIdInAndIsDeletedFalseOrderByTaskIdAscVersionDesc(sanitized);

        // 批量查询任务基础信息（投影查询，仅查必要字段）
        Map<Long, TaskBasicInfo> taskMap = new HashMap<>();
        if (!sanitized.isEmpty()) {
            try {
                List<Object[]> taskBasicInfos = taskRepository.findBasicInfoByIdInAndIsDeletedFalse(sanitized);
                taskMap = taskBasicInfos.stream()
                        .collect(Collectors.toMap(
                                arr -> (Long) arr[0],
                                arr -> new TaskBasicInfo(
                                        (Long) arr[0],
                                        (String) arr[1],
                                        (Long) arr[2],
                                        (LocalDate) arr[3]
                                )
                        ));
            } catch (Exception e) {
                log.warn("批量查询任务基本信息失败: taskIds={}", sanitized, e);
            }
        }

        // 批量查询项目名称（投影查询）
        List<Long> projectIds = submissions.stream()
                .map(TaskSubmission::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> projectNameMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            try {
                for (Long projectId : projectIds) {
                    projectRepository.findProjectNameById(projectId)
                            .ifPresent(name -> projectNameMap.put(projectId, name));
                }
            } catch (Exception e) {
                log.warn("批量查询项目名称失败: projectIds={}", projectIds, e);
            }
        }

        // 初始化结果Map（确保所有请求的任务ID都存在）
        final Map<Long, TaskBasicInfo> finalTaskMap = taskMap;
        Map<String, List<TaskSubmissionDTO>> result = new LinkedHashMap<>();
        for (Long taskId : sanitized) {
            result.put(String.valueOf(taskId), new ArrayList<>());
        }

        // 转换提交记录为DTO并分组
        for (TaskSubmission submission : submissions) {
            Long taskId = submission.getTaskId();
            TaskBasicInfo taskBasicInfo = finalTaskMap.get(taskId);

            // 构建临时Task对象用于DTO转换
            Task task = null;
            if (taskBasicInfo != null) {
                task = Task.builder()
                        .id(taskBasicInfo.getId())
                        .title(taskBasicInfo.getTitle())
                        .creatorId(taskBasicInfo.getCreatorId())
                        .dueDate(taskBasicInfo.getDueDate())
                        .build();
            }

            TaskSubmissionDTO dto = convertToDTO(submission, task);
            result.computeIfAbsent(String.valueOf(taskId), k -> new ArrayList<>()).add(dto);
        }

        return result;
    }

    /**
     * 批量查询任务的附件URL（按任务ID分组，去重）
     * <p>确保所有请求的任务ID都在结果中（无附件则返回空列表）
     *
     * @param taskIds 任务ID列表
     * @return 键：任务ID字符串；值：该任务的附件URL列表（去重）
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getTasksAttachments(List<Long> taskIds) {
        log.info("批量查询任务附件: taskIds={}", taskIds);

        if (taskIds == null || taskIds.isEmpty()) {
            return new HashMap<>();
        }

        // 查询所有任务的提交记录
        List<TaskSubmission> submissions = submissionRepository
                .findByTaskIdInAndIsDeletedFalseOrderByTaskIdAscVersionDesc(taskIds);

        // 按任务ID分组收集附件URL（去重）
        Map<String, List<String>> result = new HashMap<>();
        for (TaskSubmission submission : submissions) {
            String taskIdStr = String.valueOf(submission.getTaskId());
            List<String> attachmentUrls = new ArrayList<>();

            // 反序列化附件URL
            if (submission.getAttachmentUrls() != null && !submission.getAttachmentUrls().trim().isEmpty()) {
                try {
                    attachmentUrls = objectMapper.readValue(
                            submission.getAttachmentUrls(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                } catch (JsonProcessingException e) {
                    log.warn("任务附件URL反序列化失败: taskId={}, submissionId={}",
                            submission.getTaskId(), submission.getId(), e);
                }
            }

            // 初始化列表，添加并去重
            result.putIfAbsent(taskIdStr, new ArrayList<>());
            List<String> existingUrls = result.get(taskIdStr);
            for (String url : attachmentUrls) {
                if (url != null && !url.trim().isEmpty() && !existingUrls.contains(url)) {
                    existingUrls.add(url);
                }
            }
        }

        // 确保所有请求的任务ID都在结果中
        for (Long taskId : taskIds) {
            String taskIdStr = String.valueOf(taskId);
            result.putIfAbsent(taskIdStr, new ArrayList<>());
        }

        log.info("批量查询任务附件完成: 查询了{}个任务，找到{}个有附件的任务",
                taskIds.size(), result.values().stream().filter(list -> !list.isEmpty()).count());

        return result;
    }

    // ======================== 分页读操作（只读事务） ========================

    /**
     * 获取当前用户待处理的提交记录（待审核）
     *
     * @param userId    用户ID
     * @param pageable  分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getPendingSubmissions(Long userId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findPendingSubmissionsForUser(userId, ReviewStatus.PENDING, pageable);
        return convertToDTOPage(submissionPage);
    }

    /**
     * 获取指定审核人审核通过的提交记录
     *
     * @param reviewerId 审核人ID
     * @param pageable   分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getSubmissionsByReviewer(Long reviewerId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findByReviewerIdAndReviewStatusAndIsDeletedFalseOrderByReviewTimeDesc(reviewerId, ReviewStatus.APPROVED, pageable);
        return convertToDTOPage(submissionPage);
    }

    /**
     * 获取当前用户创建的任务中，已审核通过的提交记录
     *
     * @param userId   用户ID（任务创建者）
     * @param pageable 分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getReviewedSubmissionsForMyCreatedTasks(Long userId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findReviewedSubmissionsForMyCreatedTasks(userId, ReviewStatus.APPROVED, pageable);
        return convertToDTOPage(submissionPage);
    }

    /**
     * 获取指定项目下待审核的提交记录
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getProjectPendingSubmissions(Long projectId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findByProjectIdAndReviewStatusAndIsDeletedFalseOrderBySubmissionTimeDesc(
                        projectId, ReviewStatus.PENDING, pageable);
        return convertToDTOPage(submissionPage);
    }

    /**
     * 获取指定用户提交的所有任务记录
     *
     * @param userId   提交人ID
     * @param pageable 分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getUserSubmissions(Long userId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findBySubmitterIdAndIsDeletedFalseOrderBySubmissionTimeDesc(userId, pageable);
        return convertToDTOPage(submissionPage);
    }

    /**
     * 获取当前用户创建的任务中，待审核的提交记录
     *
     * @param userId   用户ID（任务创建者）
     * @param pageable 分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getMyCreatedTasksPendingSubmissions(Long userId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findPendingSubmissionsForMyCreatedTasks(userId, ReviewStatus.PENDING, pageable);
        return convertToDTOPage(submissionPage);
    }

    /**
     * 获取当前用户待提交审核的记录（自己提交的待审核）
     *
     * @param userId   用户ID（提交人）
     * @param pageable 分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getMyPendingSubmissions(Long userId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findMyPendingSubmissions(userId, ReviewStatus.PENDING, pageable);
        return convertToDTOPage(submissionPage);
    }

    /**
     * 获取当前用户需要审核的待审核提交记录
     *
     * @param userId   用户ID（审核人）
     * @param pageable 分页参数
     * @return 分页的提交记录DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TaskSubmissionDTO> getPendingSubmissionsForReview(Long userId, Pageable pageable) {
        Page<TaskSubmission> submissionPage = submissionRepository
                .findPendingSubmissionsForReviewer(userId, ReviewStatus.PENDING, pageable);
        return convertToDTOPage(submissionPage);
    }

    // ======================== 统计/计数操作 ========================

    /**
     * 获取任务提交统计信息
     * <p>包含：执行者数量、总提交数、审核通过数、按执行者分组统计、是否可标记为完成
     *
     * @param taskId 任务ID
     * @return 统计信息Map
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTaskSubmissionStats(Long taskId) {
        Map<String, Object> stats = new HashMap<>();

        // 统计执行者数量
        List<TaskUser> executors = taskUserRepository.findActiveExecutorsByTaskId(taskId);
        stats.put("totalExecutors", executors.size());

        // 统计所有提交记录
        List<TaskSubmission> allSubmissions = submissionRepository
                .findByTaskIdAndIsDeletedFalseOrderByVersionDesc(taskId);
        stats.put("totalSubmissions", allSubmissions.size());

        // 统计审核通过的提交记录
        List<TaskSubmission> approvedSubmissions = allSubmissions.stream()
                .filter(s -> s.getReviewStatus() == ReviewStatus.APPROVED)
                .toList();
        stats.put("approvedSubmissions", approvedSubmissions.size());

        // 按执行者分组统计提交记录
        Map<Long, List<TaskSubmission>> submissionsByExecutor = allSubmissions.stream()
                .collect(Collectors.groupingBy(TaskSubmission::getSubmitterId));
        stats.put("submissionsByExecutor", submissionsByExecutor);

        // 判断是否可标记为完成（有审核通过的提交）
        stats.put("canBeMarkedAsDone", !approvedSubmissions.isEmpty());

        return stats;
    }

    /**
     * 统计当前用户待处理的提交记录数（待审核）
     *
     * @param userId 用户ID
     * @return 待审核提交记录数
     */
    @Override
    public long countPendingSubmissions(Long userId) {
        return submissionRepository.countPendingSubmissionsForUser(userId, ReviewStatus.PENDING);
    }

    /**
     * 统计指定项目下待审核的提交记录数
     *
     * @param projectId 项目ID
     * @return 待审核提交记录数
     */
    @Override
    public long countProjectPendingSubmissions(Long projectId) {
        return submissionRepository.countByProjectIdAndReviewStatusAndIsDeletedFalse(
                projectId, ReviewStatus.PENDING);
    }

    /**
     * 统计当前用户创建的任务中，待审核的提交记录数
     *
     * @param userId 用户ID（任务创建者）
     * @return 待审核提交记录数
     */
    @Override
    public long countMyCreatedTasksPendingSubmissions(Long userId) {
        return submissionRepository.countPendingSubmissionsForMyCreatedTasks(userId, ReviewStatus.PENDING);
    }

    /**
     * 统计当前用户待提交审核的记录数（自己提交的待审核）
     *
     * @param userId 用户ID（提交人）
     * @return 待审核提交记录数
     */
    @Override
    public long countMyPendingSubmissions(Long userId) {
        return submissionRepository.countMyPendingSubmissions(userId, ReviewStatus.PENDING);
    }

    /**
     * 统计当前用户需要审核的待审核提交记录数
     *
     * @param userId 用户ID（审核人）
     * @return 待审核提交记录数
     */
    @Override
    public long countPendingSubmissionsForReview(Long userId) {
        return submissionRepository.countPendingSubmissionsForReviewer(userId, ReviewStatus.PENDING);
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 分页提交记录转换为DTO分页对象
     * <p>核心优化：批量查询任务/项目/用户基础信息，避免N+1查询
     *
     * @param submissionPage 提交记录分页对象
     * @return DTO分页对象
     */
    private Page<TaskSubmissionDTO> convertToDTOPage(Page<TaskSubmission> submissionPage) {
        List<TaskSubmission> submissions = submissionPage.getContent();
        if (submissions == null || submissions.isEmpty()) {
            return Page.empty(submissionPage.getPageable());
        }

        // 1. 批量查询任务基础信息（投影查询，仅查必要字段）
        List<Long> taskIds = submissions.stream()
                .map(TaskSubmission::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, TaskBasicInfo> taskMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            try {
                List<Object[]> taskBasicInfos = taskRepository.findBasicInfoByIdInAndIsDeletedFalse(taskIds);
                taskMap = taskBasicInfos.stream()
                        .collect(Collectors.toMap(
                                arr -> (Long) arr[0],
                                arr -> new TaskBasicInfo(
                                        (Long) arr[0],
                                        (String) arr[1],
                                        (Long) arr[2],
                                        (LocalDate) arr[3]
                                )
                        ));
            } catch (Exception e) {
                log.warn("批量查询任务基本信息失败: taskIds={}", taskIds, e);
            }
        }

        // 2. 批量查询项目名称（投影查询）
        List<Long> projectIds = submissions.stream()
                .map(TaskSubmission::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> projectNameMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            try {
                for (Long projectId : projectIds) {
                    projectRepository.findProjectNameById(projectId)
                            .ifPresent(name -> projectNameMap.put(projectId, name));
                }
            } catch (Exception e) {
                log.warn("批量查询项目名称失败: projectIds={}", projectIds, e);
            }
        }

        // 3. 批量查询用户基础信息（避免全字段查询）
        Set<Long> userIds = new HashSet<>();
        for (TaskSubmission submission : submissions) {
            if (submission.getSubmitterId() != null) {
                userIds.add(submission.getSubmitterId());
            }
            if (submission.getReviewerId() != null) {
                userIds.add(submission.getReviewerId());
            }
        }
        Map<Long, UserBasicInfo> userBasicInfoMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                List<UserBasicInfo> userInfos = userRepository.findBasicInfoByIdInAndIsDeletedFalse(new ArrayList<>(userIds));
                userBasicInfoMap = userInfos.stream()
                        .collect(Collectors.toMap(UserBasicInfo::getId, info -> info));
            } catch (Exception e) {
                log.warn("批量查询用户信息失败: userIds={}", userIds, e);
            }
        }

        // 4. 转换为DTO列表
        final Map<Long, TaskBasicInfo> finalTaskMap = taskMap;
        final Map<Long, String> finalProjectNameMap = projectNameMap;
        final Map<Long, UserBasicInfo> finalUserBasicInfoMap = userBasicInfoMap;

        List<TaskSubmissionDTO> dtoList = submissions.stream()
                .map(submission -> {
                    // 反序列化附件URL
                    List<String> attachmentUrls = new ArrayList<>();
                    if (submission.getAttachmentUrls() != null) {
                        try {
                            attachmentUrls = objectMapper.readValue(
                                    submission.getAttachmentUrls(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                            );
                        } catch (JsonProcessingException e) {
                            log.warn("附件URL反序列化失败", e);
                        }
                    }

                    // 获取项目名称
                    String projectName = finalProjectNameMap.get(submission.getProjectId());

                    // 转换提交人/审核人基础信息为DTO
                    UserDTO submitterDTO = convertUserBasicInfoToDTO(finalUserBasicInfoMap.get(submission.getSubmitterId()));
                    UserDTO reviewerDTO = convertUserBasicInfoToDTO(finalUserBasicInfoMap.get(submission.getReviewerId()));

                    // 获取任务基础信息
                    TaskBasicInfo taskBasicInfo = finalTaskMap.get(submission.getTaskId());
                    String taskTitle = taskBasicInfo != null ? taskBasicInfo.getTitle() : null;
                    String taskCreatorId = taskBasicInfo != null && taskBasicInfo.getCreatorId() != null
                            ? String.valueOf(taskBasicInfo.getCreatorId())
                            : null;
                    LocalDate dueDate = taskBasicInfo != null ? taskBasicInfo.getDueDate() : null;

                    // 构建DTO并返回
                    return TaskSubmissionDTO.builder()
                            .id(String.valueOf(submission.getId()))
                            .taskId(String.valueOf(submission.getTaskId()))
                            .taskTitle(taskTitle)
                            .taskCreatorId(taskCreatorId)
                            .projectId(String.valueOf(submission.getProjectId()))
                            .projectName(projectName)
                            .submitterId(String.valueOf(submission.getSubmitterId()))
                            .submitter(submitterDTO)
                            .submissionContent(submission.getSubmissionContent())
                            .attachmentUrls(attachmentUrls)
                            .submissionTime(instantToLocalDateTime(submission.getSubmissionTime()))
                            .reviewStatus(submission.getReviewStatus())
                            .reviewerId(submission.getReviewerId() != null ? String.valueOf(submission.getReviewerId()) : null)
                            .reviewer(reviewerDTO)
                            .reviewComment(submission.getReviewComment())
                            .reviewTime(instantToLocalDateTime(submission.getReviewTime()))
                            .actualWorktime(submission.getActualWorktime())
                            .version(submission.getVersion())
                            .createdAt(instantToLocalDateTime(submission.getCreatedAt()))
                            .updatedAt(instantToLocalDateTime(submission.getUpdatedAt()))
                            .dueDate(dueDate)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, submissionPage.getPageable(), submissionPage.getTotalElements());
    }

    /**
     * 转换提交记录为DTO（带关联任务）
     *
     * @param submission 提交记录实体
     * @param task       关联任务实体
     * @return 提交记录DTO
     */
    private TaskSubmissionDTO convertToDTO(TaskSubmission submission, Task task) {
        // 反序列化附件URL
        List<String> attachmentUrls = new ArrayList<>();
        if (submission.getAttachmentUrls() != null) {
            try {
                attachmentUrls = objectMapper.readValue(
                        submission.getAttachmentUrls(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
            } catch (JsonProcessingException e) {
                log.warn("附件URL反序列化失败", e);
            }
        }

        // 获取项目名称
        String projectName = null;
        try {
            if (submission.getProjectId() != null) {
                Optional<Project> projectOpt = projectRepository.findById(submission.getProjectId());
                projectName = projectOpt.map(Project::getName).orElse(null);
            }
        } catch (Exception e) {
            log.warn("获取项目名称失败，projectId: {}", submission.getProjectId(), e);
        }

        // 获取提交人信息
        UserDTO submitter = null;
        try {
            if (submission.getSubmitterId() != null) {
                Optional<User> userOpt = userRepository.findById(submission.getSubmitterId());
                submitter = userOpt.map(userConverter::toDTO).orElse(null);
            }
        } catch (Exception e) {
            log.warn("获取提交人信息失败，submitterId: {}", submission.getSubmitterId(), e);
        }

        // 获取审核人信息
        UserDTO reviewer = null;
        try {
            if (submission.getReviewerId() != null) {
                Optional<User> userOpt = userRepository.findById(submission.getReviewerId());
                reviewer = userOpt.map(userConverter::toDTO).orElse(null);
            }
        } catch (Exception e) {
            log.warn("获取审核人信息失败，reviewerId: {}", submission.getReviewerId(), e);
        }

        // 构建并返回DTO
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
                .dueDate(task != null ? task.getDueDate() : null)
                .build();
    }

    /**
     * 转换提交记录为DTO（自动查询关联任务基础信息）
     *
     * @param submission 提交记录实体
     * @return 提交记录DTO
     */
    private TaskSubmissionDTO convertToDTO(TaskSubmission submission) {
        // 查询任务基础信息
        Task task = null;
        try {
            Optional<Object[]> taskBasicInfoOpt = taskRepository.findBasicInfoById(submission.getTaskId());
            if (taskBasicInfoOpt.isPresent()) {
                Object[] arr = taskBasicInfoOpt.get();
                task = Task.builder()
                        .id(submission.getTaskId())
                        .title((String) arr[0])
                        .creatorId((Long) arr[1])
                        .dueDate((LocalDate) arr[2])
                        .build();
            }
        } catch (Exception e) {
            log.warn("获取任务基本信息失败，taskId: {}", submission.getTaskId(), e);
        }
        return convertToDTO(submission, task);
    }

    /**
     * 将UserBasicInfo转换为UserDTO（仅包含基础字段）
     *
     * @param userBasicInfo 用户基础信息
     * @return 用户DTO（仅基础字段）
     */
    private UserDTO convertUserBasicInfoToDTO(UserBasicInfo userBasicInfo) {
        if (userBasicInfo == null) {
            return null;
        }
        return UserDTO.builder()
                .id(userBasicInfo.getId())
                .name(userBasicInfo.getName())
                .email(userBasicInfo.getEmail())
                .avatarUrl(userBasicInfo.getAvatarUrl())
                .build();
    }

    /**
     * Instant转换为LocalDateTime（适配系统时区）
     *
     * @param instant 时间戳
     * @return 本地时间
     */
    private LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}