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
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/zhiyan/ai/achievement/generate")
@RequiredArgsConstructor
@Tag(name = "任务成果AI生成", description = "AI生成任务成果草稿相关接口")
public class TaskResultAIGenerateController {

    private final TaskResultAIGenerateService aiGenerateService;
    private final TaskSubmissionService taskSubmissionService;
    private final AchievementDetailsService achievementDetailsService;
    private final AchievementTaskService achievementTaskService;
    private final AchievementFileService achievementFileService;

    @PostMapping("/draft")
    @Operation(summary = "生成任务成果草稿", description = "根据任务信息生成任务成果草稿，异步任务")
    public R<TaskResultGenerateResponse> generateDraft(
            @Parameter(description = "生成请求") @RequestBody TaskResultGenerateRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法生成任务成果草稿");
        }
        request.setUserId(userId);
        String jobId = aiGenerateService.generateTaskResultDraft(request);

        TaskResultGenerateResponse response = TaskResultGenerateResponse.builder()
                .jobId(jobId)
                .status("PENDING")
                .progress(0)
                .userId(userId)
                .projectId(request.getProjectId())
                .build();

        return R.ok(response, "生成任务已提交，请稍后查询状态");
    }

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
                .map(id -> getTaskResultContext(id).getData())
                .toList());
    }

    @PostMapping("/{jobId}/save")
    @Operation(summary = "保存AI任务成果为知识库成果", description = "将已完成的AI任务成果草稿保存为知识库中的任务成果(TASK_RESULT)，并建立与任务的关联")
    public R<AchievementDTO> saveAIDraftAsAchievement(
            @Parameter(description = "生成任务ID") @PathVariable String jobId) {
        Long userId = SecurityUtils.getUserId();
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
        if (projectId == null) {
            return R.fail("生成记录缺少项目ID，无法创建成果");
        }

        List<Long> taskIds = generateResponse.getTaskIds();

        String markdown = String.valueOf(draftContent.get("markdown"));

        String title = generateResponse.getAchievementTitle();
        if (title == null || title.isBlank()) {
            title = "任务成果-" + LocalDateTime.now();
        }

        String abstractText = markdown.length() > 200 ? markdown.substring(0, 200) : markdown;

        Map<String, Object> detailData = new HashMap<>();
        detailData.put("contentMarkdown", markdown);
        detailData.put("source", "AI_GENERATED");
        detailData.put("jobId", jobId);
        if (taskIds != null) {
            detailData.put("taskIds", taskIds);
        }

        CreateAchievementDTO createDTO = CreateAchievementDTO.builder()
                .projectId(projectId)
                .title(title)
                .type(AchievementType.TASK_RESULT)
                .status(AchievementStatus.draft)
                .isPublic(false)
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
                uploadTaskAttachmentsAsAchievementFiles(achievementId, taskIds, userId);
            } catch (NumberFormatException ignored) {
                // 如果ID无法解析为Long，跳过关联，但不影响成果创建
            }
        }

        return R.ok(achievementDTO, "AI任务成果已保存为知识库成果");
    }

    private void uploadTaskAttachmentsAsAchievementFiles(Long achievementId, List<Long> taskIds, Long userId) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        Set<String> attachmentUrls = new LinkedHashSet<>();

        for (Long taskId : taskIds) {
            try {
                List<TaskSubmissionDTO> submissions = taskSubmissionService.getTaskSubmissions(taskId);
                TaskSubmissionDTO finalApproved = findFinalApprovedSubmission(submissions);
                TaskSubmissionDTO latest = findLatestSubmission(submissions);
                TaskSubmissionDTO source = finalApproved != null ? finalApproved : latest;

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
                .max(Comparator.comparing(TaskSubmissionDTO::getSubmissionTime, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private TaskSubmissionDTO findFinalApprovedSubmission(List<TaskSubmissionDTO> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return null;
        }
        return submissions.stream()
                .filter(s -> s.getReviewStatus() == ReviewStatus.APPROVED)
                .findFirst()
                .orElse(null);
    }

    private MultipartFile downloadAsMultipart(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = inputStream.readAllBytes();
            String fileName = extractFileNameFromUrl(fileUrl);

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
        return "file";
    }
}
