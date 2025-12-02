package hbnu.project.zhiyanbackend.tasks.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskDetailDTO;
import hbnu.project.zhiyanbackend.tasks.model.dto.UserTaskStatisticsDTO;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskPriority;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import hbnu.project.zhiyanbackend.tasks.model.form.CreateTaskRequest;
import hbnu.project.zhiyanbackend.tasks.model.form.UpdateTaskRequest;
import hbnu.project.zhiyanbackend.tasks.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理接口
 *
 * @author Tokito
 */

@RestController
@RequestMapping("/zhiyan/projects/tasks")
@Tag(name = "任务管理", description = "任务创建、分配等接口")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "创建任务")
    public R<Task> createTask(@Valid @RequestBody CreateTaskRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法创建任务");
        }
        return taskService.createTask(request, userId);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "更新任务")
    public R<Task> updateTask(@PathVariable("taskId") Long taskId,
                              @Valid @RequestBody UpdateTaskRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法更新任务");
        }
        return taskService.updateTask(taskId, request, userId);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务（软删除）")
    public R<Void> deleteTask(@PathVariable("taskId") Long taskId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法删除任务");
        }
        return taskService.deleteTask(taskId, userId);
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "更新任务状态")
    public R<Task> updateTaskStatus(@PathVariable("taskId") Long taskId,
                                    @RequestParam("status") TaskStatus status) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法更新任务状态");
        }
        return taskService.updateTaskStatus(taskId, status, userId);
    }

    @PutMapping("/{taskId}/assign")
    @Operation(summary = "分配任务执行者")
    public R<Task> assignTask(@PathVariable("taskId") Long taskId,
                              @RequestBody List<Long> assigneeIds) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法分配任务");
        }
        return taskService.assignTask(taskId, assigneeIds, userId);
    }

    @PostMapping("/{taskId}/claim")
    @Operation(summary = "接取任务")
    public R<Task> claimTask(@PathVariable("taskId") Long taskId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法接取任务");
        }
        return taskService.claimTask(taskId, userId);
    }

    @PostMapping("/{taskId}/cancel-assignees")
    @Operation(summary = "取消任务负责人并重置为待办")
    public R<Task> cancelTaskAssignees(@PathVariable("taskId") Long taskId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法取消任务负责人");
        }
        return taskService.cancelTaskAssignees(taskId, userId);
    }

    @GetMapping("/projects/{projectId}")
    @Operation(summary = "分页获取项目任务列表（包含执行者信息）")
    public R<Page<TaskDetailDTO>> getProjectTasks(@PathVariable("projectId") Long projectId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getProjectTasksWithAssignees(projectId, pageable);
    }

    @GetMapping("/projects/{projectId}/status/{status}")
    @Operation(summary = "按状态分页获取项目任务")
    public R<Page<Task>> getTasksByStatus(@PathVariable("projectId") Long projectId,
                                          @PathVariable("status") TaskStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getTasksByStatus(projectId, status, pageable);
    }

    @GetMapping("/projects/{projectId}/priority/{priority}")
    @Operation(summary = "按优先级分页获取项目任务")
    public R<Page<Task>> getTasksByPriority(@PathVariable("projectId") Long projectId,
                                            @PathVariable("priority") TaskPriority priority,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getTasksByPriority(projectId, priority, pageable);
    }

    @GetMapping("/my-assigned")
    @Operation(summary = "分页获取我参与的任务")
    public R<Page<Task>> getMyAssignedTasks(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取我的任务");
        }
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getMyAssignedTasks(userId, pageable);
    }

    @GetMapping("/my-created")
    @Operation(summary = "分页获取我创建的任务")
    public R<Page<Task>> getMyCreatedTasks(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取我创建的任务");
        }
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getMyCreatedTasks(userId, pageable);
    }

    @GetMapping("/my-to-review")
    @Operation(summary = "分页获取待我审核的任务")
    public R<Page<Task>> getMyTasksToReview(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取待审核任务");
        }
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getMyTasksToReview(userId, pageable);
    }

    @GetMapping("/projects/{projectId}/search")
    @Operation(summary = "搜索项目任务")
    public R<Page<Task>> searchTasks(@PathVariable("projectId") Long projectId,
                                     @RequestParam("keyword") String keyword,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskService.searchTasks(projectId, keyword, pageable);
    }

    @GetMapping("/projects/{projectId}/upcoming")
    @Operation(summary = "获取项目中即将到期的任务")
    public R<Page<Task>> getUpcomingTasks(@PathVariable("projectId") Long projectId,
                                          @RequestParam(defaultValue = "7") int days,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getUpcomingTasks(projectId, days, pageable);
    }

    @GetMapping("/projects/{projectId}/overdue")
    @Operation(summary = "获取项目中已逾期的任务")
    public R<Page<Task>> getOverdueTasks(@PathVariable("projectId") Long projectId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getOverdueTasks(projectId, pageable);
    }

    @GetMapping("/my-upcoming")
    @Operation(summary = "获取我即将到期的任务")
    public R<Page<Task>> getMyUpcomingTasks(@RequestParam(defaultValue = "7") int days,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取我的即将到期任务");
        }
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getMyUpcomingTasks(userId, days, pageable);
    }

    @GetMapping("/my-overdue")
    @Operation(summary = "获取我的已逾期任务")
    public R<Page<Task>> getMyOverdueTasks(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取我的已逾期任务");
        }
        Pageable pageable = PageRequest.of(page, size);
        return taskService.getMyOverdueTasks(userId, pageable);
    }

    @GetMapping("/my-statistics")
    @Operation(summary = "获取我的任务统计信息")
    public R<UserTaskStatisticsDTO> getMyTaskStatistics() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取任务统计信息");
        }
        return taskService.getUserTaskStatistics(userId);
    }
}
