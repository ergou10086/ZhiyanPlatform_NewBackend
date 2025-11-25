package hbnu.project.zhiyanbackend.tasks.service;

import hbnu.project.zhiyanbackend.basic.domain.R;
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
 */
public interface TaskService {

    R<Task> createTask(CreateTaskRequest request, Long creatorId);

    R<Task> updateTask(Long taskId, UpdateTaskRequest request, Long operatorId);

    R<Void> deleteTask(Long taskId, Long operatorId);

    R<Task> updateTaskStatus(Long taskId, TaskStatus newStatus, Long operatorId);

    R<Task> assignTask(Long taskId, List<Long> assigneeIds, Long operatorId);

    R<Task> claimTask(Long taskId, Long userId);

    R<Page<Task>> getProjectTasks(Long projectId, Pageable pageable);

    R<Page<Task>> getTasksByStatus(Long projectId, TaskStatus status, Pageable pageable);

    R<Page<Task>> getTasksByPriority(Long projectId, TaskPriority priority, Pageable pageable);

    R<Page<Task>> getMyAssignedTasks(Long userId, Pageable pageable);

    R<Page<Task>> getMyCreatedTasks(Long userId, Pageable pageable);

    R<Page<Task>> searchTasks(Long projectId, String keyword, Pageable pageable);

    R<Task> cancelTaskAssignees(Long taskId, Long operatorId);

    R<Page<Task>> getMyTasksToReview(Long userId, Pageable pageable);

    R<Page<Task>> getUpcomingTasks(Long projectId, int days, Pageable pageable);

    R<Page<Task>> getOverdueTasks(Long projectId, Pageable pageable);

    R<Page<Task>> getMyUpcomingTasks(Long userId, int days, Pageable pageable);

    R<Page<Task>> getMyOverdueTasks(Long userId, Pageable pageable);

    R<UserTaskStatisticsDTO> getUserTaskStatistics(Long userId);
}
