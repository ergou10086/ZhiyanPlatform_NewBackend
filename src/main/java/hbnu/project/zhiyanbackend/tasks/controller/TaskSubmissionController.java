package hbnu.project.zhiyanbackend.tasks.controller;

import com.qcloud.cos.http.HttpMethodName;
import hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.enums.BizOperationModule;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.basic.utils.JwtUtils;
import hbnu.project.zhiyanbackend.oss.config.COSProperties;
import hbnu.project.zhiyanbackend.oss.service.COSService;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionFileResponse;
import hbnu.project.zhiyanbackend.tasks.model.form.ReviewSubmissionRequest;
import hbnu.project.zhiyanbackend.tasks.model.form.SubmitTaskRequest;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionService;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 任务提交控制器
 * 处理任务提交、审核相关的接口
 *
 * @author Tokito
 */

@Slf4j
@RestController
@RequestMapping("/zhiyan/projects/tasks/submissions")
@Tag(name = "任务提交管理", description = "任务提交、审核、撤回等接口")
@RequiredArgsConstructor
public class TaskSubmissionController {

    private final TaskSubmissionService submissionService;
    private final TaskSubmissionFileService fileService;
    private final JwtUtils jwtUtils;
    private final COSService cosService;
    private final COSProperties cosProperties;

    @PostMapping("/{taskId}/submit")
    @Operation(summary = "提交任务", description = "任务执行者提交任务，等待任务创建者审核")
    @BizOperationLog(module = BizOperationModule.TASK, type = "SUBMIT", description = "提交任务")
    public R<TaskSubmissionDTO> submitTask(
            @PathVariable("taskId") @Parameter(description = "任务ID") Long taskId,
            @Valid @RequestBody SubmitTaskRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法提交任务");
        }
        TaskSubmissionDTO result = submissionService.submitTask(taskId, request, userId);
        return R.ok(result, "任务提交成功，等待审核");
    }

    @PutMapping("/{submissionId}/review")
    @Operation(summary = "审核任务提交", description = "任务创建者审核任务提交（批准/拒绝）")
    @BizOperationLog(module = BizOperationModule.TASK, type = "REVIEW", description = "审核任务")
    public R<TaskSubmissionDTO> reviewSubmission(
            @PathVariable("submissionId") @Parameter(description = "提交记录ID") Long submissionId,
            @Valid @RequestBody ReviewSubmissionRequest request) {
        Long reviewerId = SecurityUtils.getUserId();
        if (reviewerId == null) {
            return R.fail("未登录或Token无效，无法审核任务");
        }
        TaskSubmissionDTO result = submissionService.reviewSubmission(submissionId, request, reviewerId);
        String message = request.getReviewStatus() != null && request.getReviewStatus().name().equals("APPROVED")
                ? "审核通过" : "审核拒绝";
        return R.ok(result, message);
    }

    @PutMapping("/{submissionId}/revoke")
    @Operation(summary = "撤回提交", description = "提交人主动撤回待审核的提交")
    public R<TaskSubmissionDTO> revokeSubmission(
            @PathVariable("submissionId") @Parameter(description = "提交记录ID") Long submissionId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法撤回提交");
        }
        TaskSubmissionDTO result = submissionService.revokeSubmission(submissionId, userId);
        return R.ok(result, "撤回成功");
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "获取提交记录详情")
    public R<TaskSubmissionDTO> getSubmissionDetail(
            @PathVariable("submissionId") @Parameter(description = "提交记录ID") Long submissionId) {
        TaskSubmissionDTO result = submissionService.getSubmissionDetail(submissionId);
        return R.ok(result);
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "获取任务提交记录", description = "查询指定任务的所有提交记录（按版本倒序）")
    public R<List<TaskSubmissionDTO>> getTaskSubmissions(
            @PathVariable("taskId") @Parameter(description = "任务ID") Long taskId) {
        List<TaskSubmissionDTO> results = submissionService.getTaskSubmissions(taskId);
        return R.ok(results);
    }

    @GetMapping("/task/{taskId}/latest")
    @Operation(summary = "获取任务最新提交", description = "查询任务的最新一次提交记录")
    public R<TaskSubmissionDTO> getLatestSubmission(
            @PathVariable("taskId") @Parameter(description = "任务ID") Long taskId) {
        TaskSubmissionDTO result = submissionService.getLatestSubmission(taskId);
        return R.ok(result);
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待审核提交列表（用户相关）")
    public R<Page<TaskSubmissionDTO>> getPendingSubmissions(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取待审核提交");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskSubmissionDTO> results = submissionService.getPendingSubmissions(userId, pageable);
        return R.ok(results);
    }

    @GetMapping("/my-pending")
    @Operation(summary = "获取我提交的待审核任务")
    public R<Page<TaskSubmissionDTO>> getMyPendingSubmissions(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取待审核任务");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskSubmissionDTO> results = submissionService.getMyPendingSubmissions(userId, pageable);
        return R.ok(results);
    }

    @GetMapping("/pending-for-review")
    @Operation(summary = "获取待我审核的提交")
    public R<Page<TaskSubmissionDTO>> getPendingSubmissionsForReview(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取待审核任务");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskSubmissionDTO> results = submissionService.getPendingSubmissionsForReview(userId, pageable);
        return R.ok(results);
    }

    @GetMapping("/project/{projectId}/pending")
    @Operation(summary = "获取项目待审核提交")
    public R<Page<TaskSubmissionDTO>> getProjectPendingSubmissions(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskSubmissionDTO> results = submissionService.getProjectPendingSubmissions(projectId, pageable);
        return R.ok(results);
    }

    @GetMapping("/my-submissions")
    @Operation(summary = "获取我的提交历史")
    public R<Page<TaskSubmissionDTO>> getMySubmissions(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取提交历史");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskSubmissionDTO> results = submissionService.getUserSubmissions(userId, pageable);
        return R.ok(results);
    }

    @GetMapping("/count/pending")
    @Operation(summary = "统计待审核提交数量（用户相关）")
    public R<Long> countPendingSubmissions() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法统计待审核提交数量");
        }
        long count = submissionService.countPendingSubmissions(userId);
        return R.ok(count);
    }

    @GetMapping("/count/my-pending")
    @Operation(summary = "统计我提交的待审核任务数量")
    public R<Long> countMyPendingSubmissions() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法统计待审核任务数量");
        }
        long count = submissionService.countMyPendingSubmissions(userId);
        return R.ok(count);
    }

    @GetMapping("/count/pending-for-review")
    @Operation(summary = "统计待我审核的提交数量")
    public R<Long> countPendingSubmissionsForReview() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法统计待审核任务数量");
        }
        long count = submissionService.countPendingSubmissionsForReview(userId);
        return R.ok(count);
    }

    @GetMapping("/count/project/{projectId}/pending")
    @Operation(summary = "统计项目待审核提交数量")
    public R<Long> countProjectPendingSubmissions(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId) {
        long count = submissionService.countProjectPendingSubmissions(projectId);
        return R.ok(count);
    }

    @GetMapping("/my-created-tasks/pending")
    @Operation(summary = "查询我创建的任务的待审核提交")
    public R<Page<TaskSubmissionDTO>> getMyCreatedTasksPendingSubmissions(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取待审核提交");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskSubmissionDTO> results = submissionService.getMyCreatedTasksPendingSubmissions(userId, pageable);
        return R.ok(results);
    }

    @GetMapping("/count/my-created-tasks/pending")
    @Operation(summary = "统计我创建的任务的待审核数量")
    public R<Long> countMyCreatedTasksPendingSubmissions() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法统计待审核提交数量");
        }
        long count = submissionService.countMyCreatedTasksPendingSubmissions(userId);
        return R.ok(count);
    }

    @GetMapping("/task/{taskId}/stats")
    @Operation(summary = "获取任务提交统计")
    public R<Map<String, Object>> getTaskSubmissionStats(
            @PathVariable("taskId") @Parameter(description = "任务ID") Long taskId) {
        Map<String, Object> stats = submissionService.getTaskSubmissionStats(taskId);
        return R.ok(stats);
    }

    @PostMapping("/tasks/attachments/batch")
    @Operation(summary = "批量查询任务附件", description = "根据任务ID列表批量查询所有任务的附件URL列表")
    public R<Map<String, List<String>>> getTasksAttachments(
            @RequestBody @Parameter(description = "任务ID列表") List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return R.fail("任务ID列表不能为空");
        }
        if (taskIds.size() > 100) {
            return R.fail("单次查询任务数量不能超过100个");
        }
        Map<String, List<String>> result = submissionService.getTasksAttachments(taskIds);
        return R.ok(result, "查询成功");
    }

    @PostMapping("/files/upload")
    @Operation(summary = "上传任务附件（单个）")
    public R<TaskSubmissionFileResponse> uploadSubmissionFile(@RequestPart("file") MultipartFile file) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法上传附件");
        }
        log.info("用户[{}]上传任务附件: filename={}, size={}, contentType={}",
                userId, file.getOriginalFilename(), file.getSize(), file.getContentType());
        TaskSubmissionFileResponse response = fileService.store(file);
        log.info("任务附件上传成功: userId={}, fileUrl={}, filename={}",
                userId, response.getUrl(), response.getFilename());
        return R.ok(response, "上传成功");
    }

    @PostMapping("/files/upload-batch")
    @Operation(summary = "上传任务附件（批量）")
    public R<List<TaskSubmissionFileResponse>> uploadSubmissionFiles(@RequestPart("files") List<MultipartFile> files) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法上传附件");
        }
        return R.ok(fileService.storeBatch(files), "上传成功");
    }

    @DeleteMapping("/files")
    @Operation(summary = "删除任务附件（单个）")
    public R<Void> deleteSubmissionFile(@RequestParam("fileUrl") String fileUrl) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法删除附件");
        }
        boolean deleted = fileService.delete(fileUrl);
        return deleted ? R.ok(null, "删除成功") : R.fail("附件不存在或已删除");
    }

    @DeleteMapping("/files/batch")
    @Operation(summary = "删除任务附件（批量）")
    public R<Map<String, Object>> deleteSubmissionFiles(@RequestBody List<String> fileUrls) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法删除附件");
        }
        int success = fileService.deleteBatch(fileUrls);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", (fileUrls == null ? 0 : fileUrls.size()) - success);
        return R.ok(result, "删除完成");
    }

    @GetMapping("/files/presigned-url")
    @Operation(summary = "获取任务附件下载链接（临时）")
    public R<Map<String, String>> getPresignedUrl(@RequestParam("fileUrl") String fileUrl) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取下载链接");
        }
        try {
            int expireMinutes = 10;
            Date expiration = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L);
            URL url = cosService.generatePresignedUrl(
                    cosProperties.getBucketName(),
                    fileUrl,
                    expiration,
                    HttpMethodName.GET,
                    null,
                    null,
                    false,
                    true
            );

            Map<String, String> result = new HashMap<>();
            result.put("url", url != null ? url.toString() : "");
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取任务附件预签名URL失败: fileUrl={}", fileUrl, e);
            return R.fail("获取下载链接失败，请稍后重试");
        }
    }

    @GetMapping("/files/download")
    @Operation(summary = "下载任务附件", description = "支持通过token参数校验的文件下载")
    public ResponseEntity<Void> downloadSubmissionFile(
            @RequestParam("fileUrl") String fileUrl,
            @RequestParam(value = "token", required = false) String token,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {

        log.info("下载任务附件请求: fileUrl={}", fileUrl);
        
        String effectiveToken = resolveToken(token, authorizationHeader);
        if (!validateToken(effectiveToken)) {
            log.warn("下载附件失败: Token验证失败, fileUrl={}", fileUrl);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 从 COS 生成预签名 URL 并重定向
        try {
            int expireMinutes = 10;
            Date expiration = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L);
            URL url = cosService.generatePresignedUrl(
                    cosProperties.getBucketName(),
                    fileUrl,
                    expiration,
                    HttpMethodName.GET,
                    null,
                    null,
                    false,
                    true
            );

            if (url != null) {
                log.info("重定向任务附件下载到COS: fileUrl={}, cosUrl={}", fileUrl, url);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url.toString()))
                        .build();
            }
        } catch (Exception e) {
            log.error("生成COS下载链接失败: fileUrl={}", fileUrl, e);
        }
        log.warn("下载附件失败: 无法生成COS预签名URL, fileUrl={}", fileUrl);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    private String resolveToken(String tokenParam, String authorizationHeader) {
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        if (token == null) {
            return false;
        }
        String subject = jwtUtils.parseToken(token);
        return subject != null;
    }

}
