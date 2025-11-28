package hbnu.project.zhiyanbackend.tasks.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import hbnu.project.zhiyanbackend.tasks.model.form.ReviewSubmissionRequest;
import hbnu.project.zhiyanbackend.tasks.model.form.SubmitTaskRequest;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务提交控制器
 * 处理任务提交、审核相关的接口
 *
 * @author Tokito
 */

@RestController
@RequestMapping("/zhiyan/projects/tasks/submissions")
@Tag(name = "任务提交管理", description = "任务提交、审核、撤回等接口")
@RequiredArgsConstructor
public class TaskSubmissionController {

    private final TaskSubmissionService submissionService;

    @PostMapping("/{taskId}/submit")
    @Operation(summary = "提交任务", description = "任务执行者提交任务，等待任务创建者审核")
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
}
