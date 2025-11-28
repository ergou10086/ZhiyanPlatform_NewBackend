package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.knowledge.model.dto.TaskResultTaskRefDTO;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementTaskRef;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementTaskRefRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementTaskService;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionDTO;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskUser;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import hbnu.project.zhiyanbackend.tasks.repository.TaskRepository;
import hbnu.project.zhiyanbackend.tasks.repository.TaskUserRepository;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionService;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 成果-任务关联服务实现
 * 负责管理成果与任务的关联关系（应用层关联）
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementTaskServiceImpl implements AchievementTaskService {

    @Resource
    private AchievementTaskRefRepository achievementTaskRefRepository;

    @Resource
    private AchievementRepository achievementRepository;

    @Resource
    private TaskRepository taskRepository;

    @Resource
    private TaskUserRepository taskUserRepository;

    @Resource
    private TaskSubmissionService taskSubmissionService;

    @Resource
    private ProjectMemberService projectMemberService;

    @Resource
    private UserRepository userRepository;

    /**
     * 关联任务到成果
     * 在创建成果时或后续编辑时关联任务
     *
     * @param achievementId 成果ID
     * @param taskIds       任务ID列表
     * @param userId        操作用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkTasksToAchievement(Long achievementId, List<Long> taskIds, Long userId) {
        log.info("关联任务到成果: achievementId={}, taskIds={}, userId={}", achievementId, taskIds, userId);

        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("任务ID列表为空，跳过关联");
            return;
        }

        // 1. 验证成果是否存在
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在: " + achievementId));

        // 2. 验证用户权限（必须是项目成员）
        if (!projectMemberService.isMember(achievement.getProjectId(), userId)) {
            throw new ServiceException("无权限操作该成果，您不是项目成员");
        }

        // 3. 验证任务是否存在且属于同一项目
        List<Task> tasks = taskRepository.findAllById(taskIds);
        if (tasks.size() != taskIds.size()) {
            throw new ServiceException("部分任务不存在");
        }

        // 4. 验证所有任务都属于同一项目
        for (Task task : tasks) {
            if (!task.getProjectId().equals(achievement.getProjectId())) {
                throw new ServiceException("任务与成果不属于同一项目，无法关联");
            }
            if (task.getIsDeleted() != null && task.getIsDeleted()) {
                throw new ServiceException("任务已删除，无法关联");
            }
        }

        // 4. 批量创建关联关系
        List<AchievementTaskRef> refs = new ArrayList<>();
        for (Long taskId : taskIds) {
            // 检查是否已存在关联
            if (achievementTaskRefRepository.findByAchievementIdAndTaskId(achievementId, taskId).isPresent()) {
                log.debug("任务已关联，跳过: achievementId={}, taskId={}", achievementId, taskId);
                continue;
            }

            AchievementTaskRef ref = AchievementTaskRef.builder()
                    .achievementId(achievementId)
                    .taskId(taskId)
                    .build();
            refs.add(ref);
        }

        if (!refs.isEmpty()) {
            achievementTaskRefRepository.saveAll(refs);
            log.info("成功关联{}个任务到成果: achievementId={}", refs.size(), achievementId);
        } else {
            log.info("所有任务都已关联，无需重复关联: achievementId={}", achievementId);
        }
    }

    /**
     * 取消关联任务
     *
     * @param achievementId 成果ID
     * @param taskIds       任务ID列表
     * @param userId        操作用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkTasksFromAchievement(Long achievementId, List<Long> taskIds, Long userId) {
        log.info("取消关联任务: achievementId={}, taskIds={}, userId={}", achievementId, taskIds, userId);

        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("任务ID列表为空，跳过取消关联");
            return;
        }

        // 1. 验证成果是否存在
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在: " + achievementId));

        // 2. 验证用户权限（必须是项目成员）
        if (!projectMemberService.isMember(achievement.getProjectId(), userId)) {
            throw new ServiceException("无权限操作该成果，您不是项目成员");
        }

        // 3. 批量删除关联关系
        int deletedCount = 0;
        for (Long taskId : taskIds) {
            try {
                achievementTaskRefRepository.deleteByAchievementIdAndTaskId(achievementId, taskId);
                deletedCount++;
            } catch (Exception e) {
                log.warn("删除关联关系失败: achievementId={}, taskId={}", achievementId, taskId, e);
            }
        }

        log.info("成功取消关联{}个任务: achievementId={}", deletedCount, achievementId);
    }

    /**
     * 获取成果关联的任务列表（带详细信息）
     * 通过查询任务服务获取任务详情
     *
     * @param achievementId 成果ID
     * @return 任务列表（包含任务详情）
     */
    @Override
    public List<TaskResultTaskRefDTO> getLinkedTasks(Long achievementId) {
        log.info("获取成果关联的任务列表: achievementId={}", achievementId);

        // 1. 查询关联关系
        List<AchievementTaskRef> refs = achievementTaskRefRepository.findByAchievementId(achievementId);
        if (refs.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 提取任务ID列表
        List<Long> taskIds = refs.stream()
                .map(AchievementTaskRef::getTaskId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量查询任务详情
        List<Task> tasks = taskRepository.findAllById(taskIds);
        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤已删除的任务
        tasks = tasks.stream()
                .filter(task -> task.getIsDeleted() == null || !task.getIsDeleted())
                .toList();

        // 4. 转换为TaskResultTaskRefDTO
        return tasks.stream()
                .map(this::convertToTaskRefDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取任务关联的成果ID列表
     *
     * @param taskId 任务ID
     * @return 成果ID列表
     */
    @Override
    public List<Long> getLinkedAchievements(Long taskId) {
        log.info("获取任务关联的成果ID列表: taskId={}", taskId);

        List<AchievementTaskRef> refs = achievementTaskRefRepository.findByTaskId(taskId);
        return refs.stream()
                .map(AchievementTaskRef::getAchievementId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 批量获取任务关联的成果ID列表
     *
     * @param taskIds 任务ID列表
     * @return Map<任务ID, 成果ID列表>
     */
    @Override
    public Map<Long, List<Long>> getLinkedAchievementsBatch(List<Long> taskIds) {
        log.info("批量获取任务关联的成果ID列表: taskIds={}", taskIds);

        if (taskIds == null || taskIds.isEmpty()) {
            return new HashMap<>();
        }

        // 批量查询关联关系
        List<AchievementTaskRef> refs = achievementTaskRefRepository.findByTaskIdIn(taskIds);

        // 按任务ID分组
        Map<Long, List<Long>> result = new HashMap<>();
        for (AchievementTaskRef ref : refs) {
            result.computeIfAbsent(ref.getTaskId(), k -> new ArrayList<>())
                    .add(ref.getAchievementId());
        }

        // 确保所有任务ID都在结果中（即使没有关联）
        for (Long taskId : taskIds) {
            result.putIfAbsent(taskId, new ArrayList<>());
        }

        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将Task转换为TaskResultTaskRefDTO
     *
     * @param task 任务实体
     * @return 任务引用DTO
     */
    private TaskResultTaskRefDTO convertToTaskRefDTO(Task task) {
        if (task == null) {
            return null;
        }

        // 1. 获取任务负责人列表
        List<TaskUser> taskUsers = taskUserRepository.findActiveExecutorsByTaskId(task.getId());
        List<Long> assigneeIds = taskUsers.stream()
                .map(TaskUser::getUserId)
                .collect(Collectors.toList());

        // 2. 获取负责人名称列表
        List<String> assigneeNames = new ArrayList<>();
        for (Long userId : assigneeIds) {
            userRepository.findNameById(userId).ifPresent(assigneeNames::add);
        }

        // 3. 获取创建者名称
        String creatorName = userRepository.findNameById(task.getCreatorId())
                .orElse("未知用户");

        // 4. 获取最新提交信息
        TaskSubmissionDTO latestSubmission = null;
        try {
            latestSubmission = taskSubmissionService.getLatestSubmission(task.getId());
        } catch (Exception e) {
            log.debug("获取任务最新提交失败: taskId={}", task.getId(), e);
        }

        // 5. 构建DTO
        TaskResultTaskRefDTO.TaskResultTaskRefDTOBuilder builder = TaskResultTaskRefDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .projectId(task.getProjectId())
                .creatorId(task.getCreatorId())
                .creatorName(creatorName)
                .assigneeNames(assigneeNames);

        // 6. 设置负责人ID列表（JSON格式）
        if (!assigneeIds.isEmpty()) {
            String assigneeIdsJson = JsonUtils.toJsonString(assigneeIds);
            builder.assigneeIds(assigneeIdsJson);
        }

        // 7. 设置最新提交信息
        if (latestSubmission != null) {
            try {
                if (latestSubmission.getId() != null && !latestSubmission.getId().isEmpty()) {
                    builder.latestSubmissionId(Long.parseLong(latestSubmission.getId()));
                }
                if (latestSubmission.getSubmitterId() != null && !latestSubmission.getSubmitterId().isEmpty()) {
                    builder.submitterId(Long.parseLong(latestSubmission.getSubmitterId()));
                }
                if (latestSubmission.getSubmitter() != null) {
                    builder.submitterName(latestSubmission.getSubmitter().getName());
                }
                if (latestSubmission.getSubmissionTime() != null) {
                    builder.latestSubmissionTime(latestSubmission.getSubmissionTime());
                }
            } catch (NumberFormatException e) {
                log.warn("解析提交信息ID失败: taskId={}", task.getId(), e);
            }
        }

        // 8. 设置任务完成时间（如果任务状态为DONE）
        if (task.getStatus() != null && task.getStatus() == TaskStatus.DONE) {
            // 如果任务已完成，使用更新时间作为完成时间
            if (task.getUpdatedAt() != null) {
                builder.completedAt(task.getUpdatedAt());
            }
        }

        return builder.build();
    }
}
