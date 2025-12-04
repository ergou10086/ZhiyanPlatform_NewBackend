package hbnu.project.zhiyanbackend.ai.aiassistant.controller;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.dto.TaskResultContextDTO;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.request.TaskResultGenerateRequest;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.TaskResultGenerateResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.TaskResultAIGenerateService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.CreateAchievementDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.UploadFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementType;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementDetailsService;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementFileService;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementTaskService;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 任务成果AI生成控制器
 * 提供AI生成任务成果草稿的相关接口，包括生成、查询、取消、保存等功能
 * @author Tokito
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/ai/achievement/generate")
@RequiredArgsConstructor
@Tag(name = "任务成果AI生成", description = "AI生成任务成果草稿相关接口")
public class TaskResultAIGenerateController {



    // 注入相关服务
    private final TaskResultAIGenerateService aiGenerateService;  // AI生成服务
    private final TaskSubmissionService taskSubmissionService;    // 任务提交服务
    private final AchievementDetailsService achievementDetailsService;  // 成果详情服务
    private final AchievementTaskService achievementTaskService;  // 成果任务服务
    private final AchievementFileService achievementFileService;  // 成果文件服务

    /**
     * 生成任务成果草稿
     * @param request 生成请求参数
     * @return 返回包含任务ID的响应
     */
    @PostMapping("/draft")
    @Operation(summary = "生成任务成果草稿", description = "根据任务信息生成任务成果草稿，异步任务")
    public R<TaskResultGenerateResponse> generateDraft(
            @Parameter(description = "生成请求") @RequestBody TaskResultGenerateRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法生成任务成果草稿");
        }
        request.setUserId(userId);
        String jobId = aiGenerateService.generateTaskResultDraft(request);  // 调用服务生成草稿

        // 构建响应对象
        TaskResultGenerateResponse response = TaskResultGenerateResponse.builder()
                .jobId(jobId)
                .status("PENDING")
                .progress(0)
                .userId(userId)
                .projectId(request.getProjectId())
                .build();

        return R.ok(response, "生成任务已提交，请稍后查询状态");
    }

    /**
     * 查询生成状态
     * @param jobId 生成任务ID
     * @return 返回生成状态和结果
     */
    @GetMapping("/status/{jobId}")
    @Operation(summary = "查询生成状态", description = "查询AI生成任务的状态和结果")
    public R<TaskResultGenerateResponse> getStatus(
            @Parameter(description = "生成任务ID") @PathVariable String jobId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法查询生成状态");
        }
        TaskResultGenerateResponse response = aiGenerateService.getGenerateStatus(jobId, userId);
        return R.ok(response);
    }

    /**
     * 取消生成任务
     * @param jobId 生成任务ID
     * @return 操作结果
     */
    @DeleteMapping("/cancel/{jobId}")
    @Operation(summary = "取消生成", description = "取消正在进行的AI生成任务")
    public R<Void> cancelGenerate(
            @Parameter(description = "生成任务ID") @PathVariable String jobId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法取消生成任务");
        }
        aiGenerateService.cancelGenerate(jobId, userId);
        return R.ok(null, "已取消生成");
    }

    /**
     * 获取AI草稿列表
     * @return 当前用户的所有AI生成草稿列表
     */
    @GetMapping("/drafts")
    @Operation(summary = "获取AI草稿列表", description = "获取当前用户的所有AI生成草稿")
    public R<List<TaskResultGenerateResponse>> getAIDrafts() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法获取AI草稿列表");
        }
        List<TaskResultGenerateResponse> drafts = aiGenerateService.getAIDrafts(userId);
        return R.ok(drafts);
    }

    @GetMapping("/task/{taskId}/context")
    @Operation(summary = "获取任务成果上下文", description = "根据任务ID获取任务详情及其所有提交记录，供AI使用")
    public R<TaskResultContextDTO> getTaskResultContext(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        List<TaskSubmissionDTO> submissions = taskSubmissionService.getTaskSubmissions(taskId);
        TaskResultContextDTO context = TaskResultContextDTO.builder()
                .task(null)
                .submissions(submissions)
                .latestSubmission(null)
                .finalApprovedSubmission(null)
                .build();
        return R.ok(context);
    }

    @GetMapping("/tasks/context")
    @Operation(summary = "获取多个任务成果上下文", description = "根据多个任务ID获取对应的任务详情及提交记录列表，供AI使用")
    public R<List<TaskResultContextDTO>> getTasksResultContext(
            @Parameter(description = "任务ID列表") @RequestParam("taskIds") List<Long> taskIds) {
        return R.ok(taskIds.stream()
    /**
     * 获取多个任务成果上下文
     * @param taskIds 任务ID列表
     * @return 多个任务对应的上下文列表
     */
                .map(id -> getTaskResultContext(id).getData())
                .toList());
    }

    @PostMapping("/{jobId}/save")
    @Operation(summary = "保存AI任务成果为知识库成果", description = "将已完成的AI任务成果草稿保存为知识库中的任务成果(TASK_RESULT)，并建立与任务的关联")
    public R<AchievementDTO> saveAIDraftAsAchievement(
            @Parameter(description = "生成任务ID") @PathVariable String jobId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = SecurityUtils.getUserId();
    /**
     * 保存AI任务成果为知识库成果
     * @param jobId 生成任务ID
     * @return 创建的成果信息
     */
        if (userId == null) {
            return R.fail("未登录，无法保存AI成果");
        }

        TaskResultGenerateResponse generateResponse = aiGenerateService.getGenerateStatus(jobId, userId);
        if (generateResponse == null || generateResponse.getStatus() == null || !"COMPLETED".equals(generateResponse.getStatus())) {
            return R.fail("生成任务未完成或不存在，无法保存为成果");
        }

        Map<String, Object> draftContent = generateResponse.getDraftContent();
        if (draftContent == null || draftContent.get("markdown") == null) {
            return R.fail("未找到可保存的AI成果内容");
        }

        Long projectId = generateResponse.getProjectId();
        // 检查草稿内容
        if (projectId == null) {
            return R.fail("生成记录缺少项目ID，无法创建成果");
        }

        List<Long> taskIds = generateResponse.getTaskIds();
        // 获取必要信息

        String markdown;
        if (body != null && body.get("markdown") != null) {
            markdown = String.valueOf(body.get("markdown"));
        } else {
            markdown = String.valueOf(draftContent.get("markdown"));
        }

        String title;
        if (body != null && body.get("title") != null) {
            title = String.valueOf(body.get("title"));
        } else {
            title = generateResponse.getAchievementTitle();
        }
        if (title == null || title.isBlank()) {
            title = "任务成果-" + LocalDateTime.now();
        }
        // 处理内容

        String abstractText = markdown.length() > 200 ? markdown.substring(0, 200) : markdown;

        Map<String, Object> detailData = new HashMap<>();
        detailData.put("contentMarkdown", markdown);
        detailData.put("source", "AI_GENERATED");
        detailData.put("jobId", jobId);
        if (taskIds != null) {
            detailData.put("taskIds", taskIds);
        // 构建成果详情数据
        }

        CreateAchievementDTO createDTO = CreateAchievementDTO.builder()
                .projectId(projectId)
                .title(title)
                .type(AchievementType.TASK_RESULT)
                .status(AchievementStatus.draft)
                .isPublic(false)
        // 创建成果
                .abstractText(abstractText)
                .linkedTaskIds(taskIds)
                .detailData(detailData)
                .creatorId(userId)
                .build();

        AchievementDTO achievementDTO = achievementDetailsService.createAchievementWithDetails(createDTO);

        if (taskIds != null && !taskIds.isEmpty()) {
            try {
                Long achievementId = Long.parseLong(achievementDTO.getId());
                achievementTaskService.linkTasksToAchievement(achievementId, taskIds, userId);

                // 同步将相关任务的最终版附件上传为该成果的文件
        // 关联任务和文件
                uploadTaskAttachmentsAsAchievementFiles(achievementId, taskIds, userId);
            } catch (NumberFormatException ignored) {
                // 如果ID无法解析为Long，跳过关联，但不影响成果创建
            }
        }

        try {
            Long achievementId = Long.parseLong(achievementDTO.getId());
            MultipartFile markdownFile = buildMarkdownFile(title, markdown);
            UploadFileDTO uploadDTO = UploadFileDTO.builder()
                    .achievementId(achievementId)
                    .uploadBy(userId)
                    .build();
            achievementFileService.uploadFile(markdownFile, uploadDTO);
        } catch (Exception e) {
            log.error("保存任务成果Markdown文件失败", e);
        }

        return R.ok(achievementDTO, "AI任务成果已保存为知识库成果");
    }

    private MultipartFile buildMarkdownFile(String title, String content) {
        byte[] bytes = content != null ? content.getBytes(StandardCharsets.UTF_8) : new byte[0];
        String safeTitle = (title == null || title.isBlank()) ? "task-result" : title.trim();
        String fileName = safeTitle + ".md";

        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "text/markdown";
            }

            @Override
            public boolean isEmpty() {
                return bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(File dest) throws IOException {
                Files.write(dest.toPath(), bytes);
            }
        };
    }

    private void uploadTaskAttachmentsAsAchievementFiles(Long achievementId, List<Long> taskIds, Long userId) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        Set<String> attachmentUrls = new LinkedHashSet<>();
    /**
     * 上传任务附件作为成果文件
     * @param achievementId 成果ID
     * @param taskIds 任务ID列表
     * @param userId 用户ID
     */

        for (Long taskId : taskIds) {
            try {
                List<TaskSubmissionDTO> submissions = taskSubmissionService.getTaskSubmissions(taskId);
                TaskSubmissionDTO finalApproved = findFinalApprovedSubmission(submissions);
                TaskSubmissionDTO latest = findLatestSubmission(submissions);
                TaskSubmissionDTO source = finalApproved != null ? finalApproved : latest;
        // 收集所有附件URL

                if (source != null && source.getAttachmentUrls() != null) {
                    attachmentUrls.addAll(source.getAttachmentUrls());
                }
            } catch (Exception e) {
                log.error("从任务提交中收集附件失败, taskId={}", taskId, e);
            }
        }

        if (attachmentUrls.isEmpty()) {
            return;
        }

        for (String url : attachmentUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            try {
                MultipartFile file = downloadAsMultipart(url);
        // 上传附件
                UploadFileDTO uploadDTO = UploadFileDTO.builder()
                        .achievementId(achievementId)
                        .uploadBy(userId)
                        .build();
                achievementFileService.uploadFile(file, uploadDTO);
            } catch (Exception e) {
                log.error("上传任务附件到成果文件失败, url={}", url, e);
            }
        }
    }

    private TaskSubmissionDTO findLatestSubmission(List<TaskSubmissionDTO> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return null;
        }
        return submissions.stream()
    /**
     * 查找最新的提交记录
     * @param submissions 提交记录列表
     * @return 最新的提交记录
     */
                .max(Comparator.comparing(TaskSubmissionDTO::getSubmissionTime, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private TaskSubmissionDTO findFinalApprovedSubmission(List<TaskSubmissionDTO> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return null;
        }
        return submissions.stream()
    /**
     * 查找最终批准的提交记录
     * @param submissions 提交记录列表
     * @return 最终批准的提交记录
     */
                .filter(s -> s.getReviewStatus() == ReviewStatus.APPROVED)
                .findFirst()
                .orElse(null);
    }

    private MultipartFile downloadAsMultipart(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = inputStream.readAllBytes();
            String fileName = extractFileNameFromUrl(fileUrl);
    /**
     * 将URL文件转换为MultipartFile
     * @param fileUrl 文件URL
     * @return MultipartFile对象
     * @throws IOException IO异常
     */

            return new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getOriginalFilename() {
                    return fileName;
                }

                @Override
                public String getContentType() {
                    return "application/octet-stream";
                }

                @Override
                public boolean isEmpty() {
                    return bytes.length == 0;
                }

                @Override
                public long getSize() {
                    return bytes.length;
                }

                @Override
                public byte[] getBytes() {
                    return bytes;
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public void transferTo(File dest) throws IOException {
                    Files.write(dest.toPath(), bytes);
                }
            };
        }
    }

    private String extractFileNameFromUrl(String url) {
        int idx = url.lastIndexOf('/');
        if (idx >= 0 && idx < url.length() - 1) {
            return url.substring(idx + 1);
        }
    /**
     * 从URL中提取文件名
     * @param url 文件URL
     * @return 文件名
     */
        return "file";
    }
}
