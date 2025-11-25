package hbnu.project.zhiyanbackend.tasks.service.impl;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.tasks.model.dto.UserTaskStatisticsDTO;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskUser;
import hbnu.project.zhiyanbackend.tasks.model.enums.AssignType;
import hbnu.project.zhiyanbackend.tasks.model.enums.RoleType;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskPriority;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import hbnu.project.zhiyanbackend.tasks.model.form.CreateTaskRequest;
import hbnu.project.zhiyanbackend.tasks.model.form.UpdateTaskRequest;
import hbnu.project.zhiyanbackend.tasks.repository.TaskRepository;
import hbnu.project.zhiyanbackend.tasks.repository.TaskUserRepository;
import hbnu.project.zhiyanbackend.tasks.service.TaskService;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskUserRepository taskUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberService projectMemberService;

    @Override
    @Transactional
    public R<Task> createTask(CreateTaskRequest request, Long creatorId) {
        Long projectId = request.getProjectId();

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return R.fail("项目不存在");
        }

        if (!projectMemberService.isMember(projectId, creatorId)) {
            return R.fail("只有项目成员才能创建任务");
        }

        List<Long> assigneeIds = request.getAssigneeIds();
        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            for (Long assigneeId : assigneeIds) {
                if (!projectMemberService.isMember(projectId, assigneeId)) {
                    return R.fail("执行者必须是项目成员");
                }
            }
            if (request.getRequiredPeople() != null && assigneeIds.size() > request.getRequiredPeople()) {
                return R.fail("分配的执行者数量不能超过任务需要人数");
            }
        }

        Task task = Task.builder()
                .projectId(projectId)
                .creatorId(creatorId)
                .title(request.getTitle())
                .description(request.getDescription())
                .worktime(request.getWorktime())
                .status(TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .requiredPeople(request.getRequiredPeople() != null ? request.getRequiredPeople() : 1)
                .isDeleted(false)
                .isMilestone(request.getIsMilestone() != null ? request.getIsMilestone() : Boolean.FALSE)
                .build();

        Task saved = taskRepository.save(task);

        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            Instant now = Instant.now();
            List<TaskUser> taskUsers = assigneeIds.stream()
                    .distinct()
                    .map(userId -> TaskUser.builder()
                            .taskId(saved.getId())
                            .projectId(projectId)
                            .userId(userId)
                            .assignType(AssignType.ASSIGNED)
                            .assignedBy(creatorId)
                            .assignedAt(now)
                            .isActive(true)
                            .roleType(RoleType.EXECUTOR)
                            .build())
                    .collect(Collectors.toList());
            taskUserRepository.saveAll(taskUsers);
        }

        return R.ok(saved, "任务创建成功");
    }

    @Override
    @Transactional
    public R<Task> updateTask(Long taskId, UpdateTaskRequest request, Long operatorId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || Boolean.TRUE.equals(taskOpt.get().getIsDeleted())) {
            return R.fail("任务不存在");
        }
        Task task = taskOpt.get();

        if (!projectMemberService.isMember(task.getProjectId(), operatorId)) {
            return R.fail("只有项目成员才能更新任务");
        }

        boolean updated = false;

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
            updated = true;
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
            updated = true;
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            updated = true;
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
            updated = true;
        }
        if (request.getRequiredPeople() != null) {
            task.setRequiredPeople(request.getRequiredPeople());
            updated = true;
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
            updated = true;
        }
        if (request.getWorktime() != null) {
            task.setWorktime(request.getWorktime());
            updated = true;
        }
        if (request.getIsMilestone() != null) {
            task.setIsMilestone(request.getIsMilestone());
            updated = true;
        }

        if (!updated) {
            return R.ok(task, "任务未发生变更");
        }

        Task saved = taskRepository.save(task);
        return R.ok(saved, "任务更新成功");
    }

    @Override
    @Transactional
    public R<Void> deleteTask(Long taskId, Long operatorId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || Boolean.TRUE.equals(taskOpt.get().getIsDeleted())) {
            return R.fail("任务不存在");
        }
        Task task = taskOpt.get();

        boolean isCreator = Objects.equals(task.getCreatorId(), operatorId);
        boolean isOwner = projectMemberService.isOwner(task.getProjectId(), operatorId);
        if (!isCreator && !isOwner) {
            return R.fail("只有任务创建者或项目负责人才能删除任务");
        }

        task.setIsDeleted(true);
        taskRepository.save(task);

        Instant now = Instant.now();
        taskUserRepository.deactivateTaskAssignees(taskId, now, operatorId);

        return R.ok(null, "任务已删除");
    }

    @Override
    @Transactional
    public R<Task> updateTaskStatus(Long taskId, TaskStatus newStatus, Long operatorId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || Boolean.TRUE.equals(taskOpt.get().getIsDeleted())) {
            return R.fail("任务不存在");
        }
        Task task = taskOpt.get();

        if (!projectMemberService.isMember(task.getProjectId(), operatorId)) {
            return R.fail("只有项目成员才能更新任务状态");
        }

        task.setStatus(newStatus);
        Task saved = taskRepository.save(task);
        return R.ok(saved, "任务状态更新成功");
    }

    @Override
    @Transactional
    public R<Task> assignTask(Long taskId, List<Long> assigneeIds, Long operatorId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || Boolean.TRUE.equals(taskOpt.get().getIsDeleted())) {
            return R.fail("任务不存在");
        }
        Task task = taskOpt.get();

        if (!projectMemberService.isMember(task.getProjectId(), operatorId)) {
            return R.fail("只有项目成员才能分配任务");
        }

        if (assigneeIds == null) {
            assigneeIds = Collections.emptyList();
        }

        List<Long> distinctAssignees = assigneeIds.stream().distinct().toList();

        for (Long assigneeId : distinctAssignees) {
            if (!projectMemberService.isMember(task.getProjectId(), assigneeId)) {
                return R.fail("执行者必须是项目成员");
            }
        }

        if (!distinctAssignees.isEmpty() && distinctAssignees.size() > task.getRequiredPeople()) {
            return R.fail("分配的执行者数量不能超过任务需要人数");
        }

        Instant now = Instant.now();

        List<TaskUser> currentAssignees = taskUserRepository.findActiveExecutorsByTaskId(taskId);
        Set<Long> currentUserIds = currentAssignees.stream()
                .map(TaskUser::getUserId)
                .collect(Collectors.toSet());

        Set<Long> newUserIds = new HashSet<>(distinctAssignees);

        Set<Long> toRemove = new HashSet<>(currentUserIds);
        toRemove.removeAll(newUserIds);

        Set<Long> toAdd = new HashSet<>(newUserIds);
        toAdd.removeAll(currentUserIds);

        if (!toRemove.isEmpty()) {
            for (Long userId : toRemove) {
                Optional<TaskUser> existing = taskUserRepository.findByTaskIdAndUserId(taskId, userId);
                if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getIsActive())) {
                    TaskUser tu = existing.get();
                    tu.setIsActive(false);
                    tu.setRemovedAt(now);
                    tu.setRemovedBy(operatorId);
                    taskUserRepository.save(tu);
                }
            }
        }

        if (!toAdd.isEmpty()) {
            List<TaskUser> newAssignees = new ArrayList<>();
            for (Long userId : toAdd) {
                Optional<TaskUser> existing = taskUserRepository.findByTaskIdAndUserId(taskId, userId);
                if (existing.isPresent()) {
                    TaskUser tu = existing.get();
                    if (!Boolean.TRUE.equals(tu.getIsActive())) {
                        tu.setIsActive(true);
                        tu.setAssignedBy(operatorId);
                        tu.setAssignedAt(now);
                        tu.setAssignType(AssignType.ASSIGNED);
                        tu.setRemovedAt(null);
                        tu.setRemovedBy(null);
                        tu.setRoleType(RoleType.EXECUTOR);
                        newAssignees.add(tu);
                    }
                } else {
                    newAssignees.add(TaskUser.builder()
                            .taskId(taskId)
                            .projectId(task.getProjectId())
                            .userId(userId)
                            .assignType(AssignType.ASSIGNED)
                            .assignedBy(operatorId)
                            .assignedAt(now)
                            .isActive(true)
                            .roleType(RoleType.EXECUTOR)
                            .build());
                }
            }
            if (!newAssignees.isEmpty()) {
                taskUserRepository.saveAll(newAssignees);
            }
        }

        if (task.getStatus() == TaskStatus.TODO && !distinctAssignees.isEmpty()) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        Task saved = taskRepository.save(task);
        return R.ok(saved, "任务分配已更新");
    }

    @Override
    @Transactional
    public R<Task> claimTask(Long taskId, Long userId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || Boolean.TRUE.equals(taskOpt.get().getIsDeleted())) {
            return R.fail("任务不存在");
        }
        Task task = taskOpt.get();

        if (!projectMemberService.isMember(task.getProjectId(), userId)) {
            return R.fail("只有项目成员才能接取任务");
        }

        if (taskUserRepository.isUserActiveExecutor(taskId, userId)) {
            return R.fail("您已经是该任务的执行者");
        }

        long currentExecutorCount = taskUserRepository.countActiveExecutorsByTaskId(taskId);
        if (currentExecutorCount >= task.getRequiredPeople()) {
            return R.fail("任务已达到最大执行人数限制");
        }

        Optional<TaskUser> existingOpt = taskUserRepository.findByTaskIdAndUserId(taskId, userId);
        Instant now = Instant.now();

        if (existingOpt.isPresent()) {
            TaskUser tu = existingOpt.get();
            tu.setIsActive(true);
            tu.setAssignedBy(userId);
            tu.setAssignedAt(now);
            tu.setAssignType(AssignType.CLAIMED);
            tu.setRemovedAt(null);
            tu.setRemovedBy(null);
            tu.setRoleType(RoleType.EXECUTOR);
            taskUserRepository.save(tu);
        } else {
            TaskUser tu = TaskUser.builder()
                    .taskId(taskId)
                    .projectId(task.getProjectId())
                    .userId(userId)
                    .assignType(AssignType.CLAIMED)
                    .assignedBy(userId)
                    .assignedAt(now)
                    .isActive(true)
                    .roleType(RoleType.EXECUTOR)
                    .build();
            taskUserRepository.save(tu);
        }

        if (task.getStatus() == TaskStatus.TODO) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        Task saved = taskRepository.save(task);
        return R.ok(saved, "任务接取成功");
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getProjectTasks(Long projectId, Pageable pageable) {
        Page<Task> page = taskRepository.findByProjectIdAndIsDeleted(projectId, false, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getTasksByStatus(Long projectId, TaskStatus status, Pageable pageable) {
        Page<Task> page = taskRepository.findByProjectIdAndStatusAndIsDeleted(projectId, status, false, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getTasksByPriority(Long projectId, TaskPriority priority, Pageable pageable) {
        Page<Task> page = taskRepository.findByProjectIdAndPriorityAndIsDeleted(projectId, priority, false, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getMyAssignedTasks(Long userId, Pageable pageable) {
        Page<TaskUser> taskUserPage = taskUserRepository.findActiveTasksByUserId(userId, pageable);
        List<Long> taskIds = taskUserPage.getContent().stream()
                .map(TaskUser::getTaskId)
                .toList();
        if (taskIds.isEmpty()) {
            return R.ok(Page.empty(pageable));
        }

        List<Task> tasks = taskRepository.findAllById(taskIds);
        Map<Long, Task> taskMap = tasks.stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .collect(Collectors.toMap(Task::getId, t -> t));

        List<Task> ordered = taskIds.stream()
                .map(taskMap::get)
                .filter(Objects::nonNull)
                .toList();

        Page<Task> result = new PageImpl<>(ordered, pageable, taskUserPage.getTotalElements());
        return R.ok(result);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getMyCreatedTasks(Long userId, Pageable pageable) {
        Page<Task> page = taskRepository.findByCreatorIdAndIsDeleted(userId, false, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getMyTasksToReview(Long userId, Pageable pageable) {
        Page<Task> page = taskRepository.findByCreatorIdAndStatusAndIsDeleted(userId, TaskStatus.PENDING_REVIEW, false, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> searchTasks(Long projectId, String keyword, Pageable pageable) {
        Page<Task> page = taskRepository.searchByKeyword(projectId, keyword, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional
    public R<Task> cancelTaskAssignees(Long taskId, Long operatorId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || Boolean.TRUE.equals(taskOpt.get().getIsDeleted())) {
            return R.fail("任务不存在");
        }
        Task task = taskOpt.get();

        if (!Objects.equals(task.getCreatorId(), operatorId)) {
            return R.fail("只有任务创建者才能取消任务负责人");
        }

        long currentExecutorCount = taskUserRepository.countActiveExecutorsByTaskId(taskId);
        if (currentExecutorCount == 0) {
            return R.fail("当前任务没有执行者，无需取消");
        }

        if (task.getStatus() == TaskStatus.DONE) {
            return R.fail("已完成的任务无法取消负责人");
        }

        Instant now = Instant.now();
        taskUserRepository.deactivateTaskAssignees(taskId, now, operatorId);

        task.setStatus(TaskStatus.TODO);
        Task saved = taskRepository.save(task);
        return R.ok(saved, "已取消任务负责人并重置为待办状态");
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getUpcomingTasks(Long projectId, int days, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(days);
        Page<Task> page = taskRepository.findUpcomingTasks(projectId, today, targetDate, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getOverdueTasks(Long projectId, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<Task> page = taskRepository.findOverdueTasks(projectId, today, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getMyUpcomingTasks(Long userId, int days, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(days);
        Page<Task> page = taskRepository.findMyUpcomingTasks(userId, today, targetDate, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<Page<Task>> getMyOverdueTasks(Long userId, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<Task> page = taskRepository.findMyOverdueTasks(userId, today, pageable);
        return R.ok(page);
    }

    @Override
    @Transactional(readOnly = true)
    public R<UserTaskStatisticsDTO> getUserTaskStatistics(Long userId) {
        List<TaskUser> allTaskUsers = taskUserRepository.findByUserIdAndIsActive(userId, true);

        long assignedCount = allTaskUsers.stream()
                .filter(tu -> tu.getAssignType() == AssignType.ASSIGNED)
                .count();

        long claimedCount = allTaskUsers.stream()
                .filter(tu -> tu.getAssignType() == AssignType.CLAIMED)
                .count();

        List<Long> taskIds = allTaskUsers.stream()
                .map(TaskUser::getTaskId)
                .toList();

        if (taskIds.isEmpty()) {
            UserTaskStatisticsDTO empty = UserTaskStatisticsDTO.builder()
                    .totalTasks(0L)
                    .assignedTasks(assignedCount)
                    .claimedTasks(claimedCount)
                    .todoTasks(0L)
                    .inProgressTasks(0L)
                    .doneTasks(0L)
                    .overdueTasks(0L)
                    .upcomingTasks(0L)
                    .projectCount(0L)
                    .build();
            return R.ok(empty);
        }

        List<Task> tasks = taskRepository.findAllById(taskIds);

        List<Task> activeTasks = tasks.stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .toList();

        long todoCount = activeTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .count();

        long inProgressCount = activeTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .count();

        long doneCount = activeTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        LocalDate today = LocalDate.now();
        long overdueCount = activeTasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> t.getDueDate().isBefore(today))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();

        LocalDate threeDaysLater = today.plusDays(3);
        long upcomingCount = activeTasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> !t.getDueDate().isBefore(today))
                .filter(t -> !t.getDueDate().isAfter(threeDaysLater))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();

        long projectCount = allTaskUsers.stream()
                .map(TaskUser::getProjectId)
                .distinct()
                .count();

        UserTaskStatisticsDTO dto = UserTaskStatisticsDTO.builder()
                .totalTasks((long) activeTasks.size())
                .assignedTasks(assignedCount)
                .claimedTasks(claimedCount)
                .todoTasks(todoCount)
                .inProgressTasks(inProgressCount)
                .doneTasks(doneCount)
                .overdueTasks(overdueCount)
                .upcomingTasks(upcomingCount)
                .projectCount(projectCount)
                .build();

        return R.ok(dto);
    }
}
