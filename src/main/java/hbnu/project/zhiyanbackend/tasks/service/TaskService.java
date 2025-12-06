package hbnu.project.zhiyanbackend.tasks.service;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskDetailDTO;
import hbnu.project.zhiyanbackend.tasks.model.dto.UserTaskStatisticsDTO;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskPriority;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import hbnu.project.zhiyanbackend.tasks.model.form.CreateTaskRequest;
import hbnu.project.zhiyanbackend.tasks.model.form.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 任务服务接口
 * 根据产品设计文档完整实现任务管理功能
 * 提供了任务的创建、更新、删除、分配、状态变更等核心功能
 * 同时支持多种查询方式，包括按状态、优先级、执行人等条件查询
 *
 * @author Tokito
 */
public interface TaskService {

    /**
     * 创建新任务
     * @param request 任务创建请求参数
     * @param creatorId 创建者ID
     * @return 创建成功的任务信息
     */
    R<Task> createTask(CreateTaskRequest request, Long creatorId);

    /**
     * 更新任务信息
     * @param taskId 任务ID
     * @param request 任务更新请求参数
     * @param operatorId 操作人ID
     * @return 更新后的任务信息
     */
    R<Task> updateTask(Long taskId, UpdateTaskRequest request, Long operatorId);

    /**
     * 删除任务
     * @param taskId 任务ID
     * @param operatorId 操作人ID
     * @return 操作结果
     */
    R<Void> deleteTask(Long taskId, Long operatorId);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param newStatus 新状态
     * @param operatorId 操作人ID
     * @return 更新后的任务信息
     */
    R<Task> updateTaskStatus(Long taskId, TaskStatus newStatus, Long operatorId);

    /**
     * 分配任务给指定用户
     * @param taskId 任务ID
     * @param assigneeIds 被分配人ID列表
     * @param operatorId 操作人ID
     * @return 分配后的任务信息
     */
    R<Task> assignTask(Long taskId, List<Long> assigneeIds, Long operatorId);

    /**
     * 认领任务
     * @param taskId 任务ID
     * @param userId 认领用户ID
     * @return 认领后的任务信息
     */
    R<Task> claimTask(Long taskId, Long userId);

    /**
     * 获取项目下的所有任务
     * @param projectId 项目ID
     * @param pageable 分页参数
     * @return 任务分页列表
     */
    R<Page<Task>> getProjectTasks(Long projectId, Pageable pageable);

    /**
     * 根据状态获取任务列表
     * @param projectId 项目ID
     * @param status 任务状态
     * @param pageable 分页参数
     * @return 指定状态的任务分页列表
     */
    R<Page<Task>> getTasksByStatus(Long projectId, TaskStatus status, Pageable pageable);

    /**
     * 根据优先级获取任务列表
     * @param projectId 项目ID
     * @param priority 任务优先级
     * @param pageable 分页参数
     * @return 指定优先级的任务分页列表
     */
    R<Page<Task>> getTasksByPriority(Long projectId, TaskPriority priority, Pageable pageable);

    /**
     * 获取我被分配的任务列表
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 被分配的任务分页列表
     */
    R<Page<Task>> getMyAssignedTasks(Long userId, Pageable pageable);

    /**
     * 获取我创建的任务列表
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 创建的任务分页列表
     */
    R<Page<Task>> getMyCreatedTasks(Long userId, Pageable pageable);

    /**
     * 搜索任务
     * @param projectId 项目ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配的任务分页列表
     */
    R<Page<Task>> searchTasks(Long projectId, String keyword, Pageable pageable);

    /**
     * 取消任务分配
     * @param taskId 任务ID
     * @param operatorId 操作人ID
     * @return 操作结果
     */
    R<Task> cancelTaskAssignees(Long taskId, Long operatorId);

    /**
     * 获取需要我审核的任务列表
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 需要审核的任务分页列表
     */
    R<Page<Task>> getMyTasksToReview(Long userId, Pageable pageable);

    /**
     * 获取项目即将到期的任务
     * @param projectId 项目ID
     * @param days 天数
     * @param pageable 分页参数
     * @return 即将到期的任务分页列表
     */
    R<Page<Task>> getUpcomingTasks(Long projectId, int days, Pageable pageable);

    /**
     * 获取项目已逾期的任务
     * @param projectId 项目ID
     * @param pageable 分页参数
     * @return 已逾期的任务分页列表
     */
    R<Page<Task>> getOverdueTasks(Long projectId, Pageable pageable);

    /**
     * 获取我即将到期的任务
     * @param userId 用户ID
     * @param days 天数
     * @param pageable 分页参数
     * @return 我即将到期的任务分页列表
     */
    R<Page<Task>> getMyUpcomingTasks(Long userId, int days, Pageable pageable);

    /**
     * 获取我已逾期的任务
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 我已逾期的任务分页列表
     */
    R<Page<Task>> getMyOverdueTasks(Long userId, Pageable pageable);

    /**
     * 获取用户的任务统计信息
     * @param userId 用户ID
     * @return 用户任务统计信息
     */
    R<UserTaskStatisticsDTO> getUserTaskStatistics(Long userId);

    /**
     * 获取项目任务列表（包含执行者信息）
     * @param projectId 项目ID
     * @param pageable 分页参数
     * @return 包含执行者信息的任务列表
     */
    R<Page<TaskDetailDTO>> getProjectTasksWithAssignees(Long projectId, Pageable pageable);
}
