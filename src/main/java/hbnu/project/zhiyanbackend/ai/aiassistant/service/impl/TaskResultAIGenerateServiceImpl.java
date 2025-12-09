package hbnu.project.zhiyanbackend.ai.aiassistant.service.impl;

import hbnu.project.zhiyanbackend.ai.aiassistant.config.DifyProperties;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.request.TaskResultGenerateRequest;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.TaskResultGenerateResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.TaskAttachmentService;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.TaskResultAIGenerateService;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.repository.TaskRepository;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskResultAIGenerateServiceImpl implements TaskResultAIGenerateService {

    private final TaskSubmissionService taskSubmissionService;
    private final TaskRepository taskRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TaskAttachmentService taskAttachmentService;
    private final DifyProperties difyProperties;
    private final RestTemplate restTemplate;

    private static final String REDIS_KEY_PREFIX = "task_result_generate:";
    private static final String USER_DRAFTS_PREFIX = "user_drafts:";
    private static final long REDIS_EXPIRE_DAYS = 7;

    @Override
    public String generateTaskResultDraft(TaskResultGenerateRequest request) {
        Long projectId = request.getProjectId();
        List<Long> taskIds = request.getTaskIds();
        Long userId = request.getUserId();

        log.info("开始生成任务成果草稿: projectId={}, taskIds={}, userId={}", projectId, taskIds, userId);

        String jobId = UUID.randomUUID().toString();

        TaskResultGenerateResponse response = TaskResultGenerateResponse.builder()
                .jobId(jobId)
                .status("PENDING")
                .progress(0)
                .userId(userId)
                .projectId(projectId)
                .taskIds(taskIds)
                .achievementTitle(request.getAchievementTitle())
                .createdAt(LocalDateTime.now())
                .build();

        String redisKey = REDIS_KEY_PREFIX + jobId;
        redisTemplate.opsForValue().set(redisKey, response, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);

        String userDraftsKey = USER_DRAFTS_PREFIX + userId;
        redisTemplate.opsForSet().add(userDraftsKey, jobId);
        redisTemplate.expire(userDraftsKey, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);

        executeGenerateTask(jobId, request);

        return jobId;
    }

    @Async
    protected void executeGenerateTask(String jobId, TaskResultGenerateRequest request) {
        String redisKey = REDIS_KEY_PREFIX + jobId;

        try {
            log.info("[JobId: {}] 开始异步生成任务成果草稿", jobId);
            updateTaskStatus(redisKey, "PROCESSING", 10, null, null);

            StringBuilder taskSummary = new StringBuilder();
            List<String> allAttachmentUrls = new ArrayList<>();

            for (Long taskId : request.getTaskIds()) {
                try {
                    log.info("[JobId: {}] 处理任务: taskId={}", jobId, taskId);
                    List<TaskSubmissionDTO> submissions = taskSubmissionService.getTaskSubmissions(taskId);

                    TaskSubmissionDTO latest = findLatestSubmission(submissions);
                    TaskSubmissionDTO approved = findFinalApprovedSubmission(submissions);

                    Optional<Task> taskOpt = taskRepository.findById(taskId);

                    String taskTitle = latest != null && latest.getTaskTitle() != null
                            ? latest.getTaskTitle()
                            : taskOpt.map(Task::getTitle).orElse("未命名任务");

                    taskSummary.append("### 任务: ").append(taskTitle).append("\n");

                    taskOpt.ifPresent(task -> {
                        taskSummary.append("**任务ID**: ").append(task.getId()).append("\n");
                        taskSummary.append("**所属项目ID**: ").append(task.getProjectId()).append("\n");
                        if (task.getStatus() != null) {
                            taskSummary.append("**任务状态**: ").append(task.getStatus().name()).append("\n");
                        }
                        if (task.getPriority() != null) {
                            taskSummary.append("**任务优先级**: ").append(task.getPriority().name()).append("\n");
                        }
                        if (task.getDueDate() != null) {
                            taskSummary.append("**截止日期**: ").append(task.getDueDate()).append("\n");
                        }
                    });

                    if (!submissions.isEmpty()) {
                        taskSummary.append("\n**提交历史概览：**\\n\n");
                        submissions.stream()
                                .sorted(Comparator.comparing(s -> Optional.ofNullable(s.getVersion()).orElse(0)))
                                .forEach(s -> taskSummary.append(String.format("- 版本 %s | 提交人: %s | 时间: %s | 审核状态: %s\n",
                                        s.getVersion() != null ? s.getVersion() : 0,
                                        s.getSubmitter() != null ? s.getSubmitter().getName() : "未知",
                                        s.getSubmissionTime() != null ? s.getSubmissionTime() : "未知",
                                        s.getReviewStatus() != null ? s.getReviewStatus().name() : "未知"))
                                );
                        taskSummary.append("\n");
                    }

                    TaskSubmissionDTO detailSource = approved != null ? approved : latest;
                    if (detailSource != null) {
                        taskSummary.append("**关键提交详情：**\\n\n");
                        taskSummary.append("- 提交人: ")
                                .append(detailSource.getSubmitter() != null ? detailSource.getSubmitter().getName() : "未知")
                                .append("\n");
                        taskSummary.append("- 提交时间: ")
                                .append(detailSource.getSubmissionTime() != null ? detailSource.getSubmissionTime() : "未知")
                                .append("\n");
                        if (detailSource.getSubmissionContent() != null) {
                            taskSummary.append("- 提交说明: ").append(detailSource.getSubmissionContent()).append("\n\n");
                        }
                    }

                    // 根据前端传来的 attachmentFilters 进行过滤
                    if (detailSource != null && detailSource.getAttachmentUrls() != null) {
                        List<String> taskAttachmentUrls = detailSource.getAttachmentUrls();
                        
                        if (request.getIncludeAttachments() && 
                            request.getAttachmentFilters() != null && 
                            !request.getAttachmentFilters().isEmpty()) {
                            
                            // 只添加用户在前端选中的附件
                            List<String> selectedUrls = taskAttachmentUrls.stream()
                                    .filter(url -> request.getAttachmentFilters().contains(url))
                                    .toList();
                            
                            allAttachmentUrls.addAll(selectedUrls);
                            
                            log.info("[JobId: {}] 任务[{}] 共有 {} 个附件，用户选中 {} 个", 
                                    jobId, taskId, taskAttachmentUrls.size(), selectedUrls.size());
                            
                            taskSummary.append("**附件数量**: ")
                                    .append(selectedUrls.size())
                                    .append(" 个（用户选中）\n\n");
                                    
                        } else if (request.getIncludeAttachments()) {
                            // 如果启用附件但没有过滤列表，则使用所有附件（向后兼容）
                            allAttachmentUrls.addAll(taskAttachmentUrls);
                            log.info("[JobId: {}] 任务[{}] 使用所有 {} 个附件（未提供过滤列表）", 
                                    jobId, taskId, taskAttachmentUrls.size());
                            taskSummary.append("**附件数量**: ")
                                    .append(taskAttachmentUrls.size())
                                    .append(" 个\n\n");
                        } else {
                            // 用户未启用附件
                            log.info("[JobId: {}] 任务[{}] 用户未启用附件", jobId, taskId);
                            taskSummary.append("**附件**: 未启用\n\n");
                        }
                    } else {
                        taskSummary.append("**附件**: 无\n\n");
                    }

                } catch (Exception e) {
                    log.error("[JobId: {}] 处理任务[{}]失败", jobId, taskId, e);
                    taskSummary.append("### 任务ID: ").append(taskId)
                            .append("\n**状态**: 处理失败 - ")
                            .append(e.getMessage())
                            .append("\n\n");
                }
            }

            List<String> allDifyFileIds = Collections.emptyList();
            if (!allAttachmentUrls.isEmpty()) {
                Set<String> uniqueUrls = new LinkedHashSet<>(allAttachmentUrls);
                log.info("[JobId: {}] 共收集到 {} 个附件 URL（去重后 {} 个），开始上传到 Dify", jobId, allAttachmentUrls.size(), uniqueUrls.size());
                allDifyFileIds = taskAttachmentService.downloadAndUploadAttachments(new ArrayList<>(uniqueUrls), request.getUserId());
                log.info("[JobId: {}] 已上传 {} 个附件到 Dify", jobId, allDifyFileIds.size());
            }

            updateTaskStatus(redisKey, "PROCESSING", 50, null, null);

            String prompt = buildPrompt(request.getAdditionalRequirements(), taskSummary.toString());
            log.info("[JobId: {}] AI提示词构建完成, 长度: {}", jobId, prompt.length());

            updateTaskStatus(redisKey, "PROCESSING", 60, null, null);

            String aiResult = callDifyAPI(prompt, allDifyFileIds, request.getUserId());

            updateTaskStatus(redisKey, "COMPLETED", 100, aiResult, null);

        } catch (Exception e) {
            log.error("[JobId: {}] 生成任务成果草稿失败", jobId, e);
            updateTaskStatus(redisKey, "FAILED", 0, null, "生成失败: " + e.getMessage());
        }
    }

    private TaskSubmissionDTO findLatestSubmission(List<TaskSubmissionDTO> submissions) {
        return submissions.stream()
                .max(Comparator.comparing(TaskSubmissionDTO::getSubmissionTime, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private TaskSubmissionDTO findFinalApprovedSubmission(List<TaskSubmissionDTO> submissions) {
        return submissions.stream()
                .filter(s -> s.getReviewStatus() == ReviewStatus.APPROVED)
                .findFirst()
                .orElse(null);
    }

    private String buildPrompt(String additionalRequirements, String taskSummary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下任务提交信息,生成一份结构化的实验成果报告:\n\n");
        prompt.append("## 任务提交信息\n\n");
        prompt.append(taskSummary);

        if (additionalRequirements != null && !additionalRequirements.isBlank()) {
            prompt.append("\n\n## 补充要求\n\n");
            prompt.append(additionalRequirements);
        }

        prompt.append("\n\n## 报告要求\n\n");
        prompt.append("请生成包含以下部分的报告:\n");
        prompt.append("1. **实验概述**: 简要说明实验目的和背景\n");
        prompt.append("2. **主要工作内容**: 详细描述完成的工作和采用的方法\n");
        prompt.append("3. **关键数据和结果**: 列出重要的实验数据、图表和结果\n");
        prompt.append("4. **问题与解决方案**: 遇到的问题及解决方法\n");
        prompt.append("5. **结论与展望**: 总结成果并提出未来工作方向\n\n");
        prompt.append("请使用Markdown格式输出,确保结构清晰、内容详实。");

        return prompt.toString();
    }

    private String callDifyAPI(String prompt, List<String> difyFileIds, Long userId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("query", prompt);
            body.put("user", String.valueOf(userId));
            body.put("inputs", new HashMap<>());

            if (difyFileIds != null && !difyFileIds.isEmpty()) {
                List<Map<String, Object>> files = new ArrayList<>();
                for (String fileId : difyFileIds) {
                    if (fileId == null || fileId.isBlank()) {
                        continue;
                    }
                    Map<String, Object> file = new HashMap<>();
                    file.put("type", "document");
                    file.put("transfer_method", "local_file");
                    file.put("upload_file_id", fileId);
                    files.add(file);
                }
                if (!files.isEmpty()) {
                    body.put("files", files);
                }
            }

            body.put("response_mode", "blocking");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(difyProperties.getApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = difyProperties.getApiUrl() + "/chat-messages";

            log.info("[Dify] 调用阻塞式接口: url={}, userId={}, files={}", url, userId,
                    difyFileIds != null ? difyFileIds.size() : 0);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<?, ?> respBody = response.getBody();
            if (respBody == null) {
                throw new RuntimeException("Dify 响应为空");
            }

            Object answer = respBody.get("answer");
            if (answer == null) {
                Object output = respBody.get("output");
                if (output instanceof Map<?, ?> outputMap) {
                    Object text = outputMap.get("text");
                    if (text != null) {
                        answer = text;
                    }
                }
            }

            if (answer == null) {
                throw new RuntimeException("Dify 响应中未找到 answer/text 字段");
            }

            return String.valueOf(answer);
        } catch (Exception e) {
            log.error("[Dify] 调用阻塞式聊天接口失败", e);
            throw new RuntimeException("AI 生成失败: " + e.getMessage(), e);
        }
    }

    private String callMockAI(String prompt) {
        return "# 任务成果报告\n\n" +
                "## 实验概述\n" +
                "根据提交的任务信息和附件,生成的实验成果报告。\n\n" +
                "## 主要工作内容\n" +
                prompt +
                "\n\n## 结论\n" +
                "任务已完成,详细内容请参考附件。";
    }

    private void updateTaskStatus(String redisKey, String status, int progress, String result, String errorMessage) {
        TaskResultGenerateResponse response = (TaskResultGenerateResponse) redisTemplate.opsForValue().get(redisKey);
        if (response != null) {
            response.setStatus(status);
            response.setProgress(progress);
            response.setUpdatedAt(LocalDateTime.now());

            if (result != null) {
                Map<String, Object> draftContent = new HashMap<>();
                draftContent.put("markdown", result);
                response.setDraftContent(draftContent);
            }
            if (errorMessage != null) {
                response.setErrorMessage(errorMessage);
            }

            redisTemplate.opsForValue().set(redisKey, response, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);
        }
    }

    @Override
    public TaskResultGenerateResponse getGenerateStatus(String jobId, Long userId) {
        String redisKey = REDIS_KEY_PREFIX + jobId;
        TaskResultGenerateResponse response = (TaskResultGenerateResponse) redisTemplate.opsForValue().get(redisKey);

        if (response == null) {
            throw new IllegalArgumentException("生成任务不存在或已过期");
        }
        if (!Objects.equals(response.getUserId(), userId)) {
            throw new IllegalArgumentException("无权查看该生成任务");
        }
        return response;
    }

    @Override
    public void cancelGenerate(String jobId, Long userId) {
        String redisKey = REDIS_KEY_PREFIX + jobId;
        TaskResultGenerateResponse response = (TaskResultGenerateResponse) redisTemplate.opsForValue().get(redisKey);

        if (response == null) {
            throw new IllegalArgumentException("生成任务不存在或已过期");
        }
        if (!Objects.equals(response.getUserId(), userId)) {
            throw new IllegalArgumentException("无权取消该生成任务");
        }

        if ("COMPLETED".equals(response.getStatus()) ||
                "FAILED".equals(response.getStatus()) ||
                "CANCELLED".equals(response.getStatus())) {
            throw new IllegalArgumentException("任务已完成或已取消,无法取消");
        }

        response.setStatus("CANCELLED");
        response.setUpdatedAt(LocalDateTime.now());
        redisTemplate.opsForValue().set(redisKey, response, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Override
    public List<TaskResultGenerateResponse> getAIDrafts(Long userId) {
        String userDraftsKey = USER_DRAFTS_PREFIX + userId;
        Set<Object> jobIds = redisTemplate.opsForSet().members(userDraftsKey);

        if (jobIds == null || jobIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<TaskResultGenerateResponse> drafts = new ArrayList<>();
        for (Object jobIdObj : jobIds) {
            String jobId = jobIdObj.toString();
            String redisKey = REDIS_KEY_PREFIX + jobId;
            TaskResultGenerateResponse response = (TaskResultGenerateResponse) redisTemplate.opsForValue().get(redisKey);
            if (response != null) {
                drafts.add(response);
            }
        }

        drafts.sort((a, b) -> {
            LocalDateTime t1 = a.getCreatedAt();
            LocalDateTime t2 = b.getCreatedAt();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        return drafts;
    }
}
