package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import hbnu.project.zhiyanbackend.knowledge.model.converter.AchievementConverter;
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
import org.springframework.cache.annotation.CacheEvict;
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

    @Resource
    private AchievementConverter achievementConverter;


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
    @CacheEvict(value = "achievementTasks", key = "#achievementId")
    public void linkTasksToAchievement(Long achievementId, List<Long> taskIds, Long userId) {
        log.info("关联任务到成果: achievementId={}, taskIds={}, userId={}", achievementId, taskIds, userId);

        // 1. 参数校验
        ValidationUtils.requireId(achievementId, "成果ID");
        ValidationUtils.requireId(userId, "用户ID");
        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("任务ID列表为空，跳过关联");
            return;
        }

        // 2. 验证成果并校验权限
        Achievement achievement = validateAchievementAndPermission(achievementId, userId);

        // 3. 验证任务是否存在且属于同一项目
        List<Task> tasks = validateTasksBatch(taskIds, achievement.getProjectId());

        // 4. 批量检查已存在的关联关系
        Set<Long> existingTaskIds = getExistingLinkedTaskIds(achievementId, taskIds);

        // 5. 批量创建关联关系
        List<AchievementTaskRef> refs = taskIds.stream()
                .filter(taskId -> !existingTaskIds.contains(taskId))
                .map(taskId -> AchievementTaskRef.builder()
                        .achievementId(achievementId)
                        .taskId(taskId)
                        .build())
                .collect(Collectors.toList());

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
    @CacheEvict(value = "achievementTasks", key = "#achievementId")
    public void unlinkTasksFromAchievement(Long achievementId, List<Long> taskIds, Long userId) {
        log.info("取消关联任务: achievementId={}, taskIds={}, userId={}", achievementId, taskIds, userId);

        // 1. 参数校验
        ValidationUtils.requireId(achievementId, "成果ID");
        ValidationUtils.requireId(userId, "用户ID");
        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("任务ID列表为空，跳过取消关联");
            return;
        }

        // 2. 验证成果并校验权限
        validateAchievementAndPermission(achievementId, userId);

        // 3. 批量删除关联关系
        int deletedCount = batchDeleteTaskLinks(achievementId, taskIds);

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

        // 4. 批量获取任务负责人信息（优化：减少查询次数）
        Map<Long, List<TaskUser>> taskUsersMap = getTaskUsersMapBatch(
                tasks.stream().map(Task::getId).collect(Collectors.toList())
        );

        // 5. 批量获取最新提交信息（优化：减少查询次数）
        Map<Long, TaskSubmissionDTO> submissionsMap = getLatestSubmissionsMapBatch(
                tasks.stream().map(Task::getId).collect(Collectors.toList())
        );

        // 6. 转换为TaskResultTaskRefDTO
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
     * 验证成果存在性并校验用户权限
     * 提取重复的权限校验逻辑
     *
     * @param achievementId 成果ID
     * @param userId        用户ID
     * @return 成果实体
     * @throws ServiceException 成果不存在或用户无权限时抛出
     */
    private Achievement validateAchievementAndPermission(Long achievementId, Long userId) {
        // 验证成果是否存在
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在: " + achievementId));

        // 验证用户权限（必须是项目成员）
        ValidationUtils.assertTrue(
                projectMemberService.isMember(achievement.getProjectId(), userId),
                "无权限操作该成果，您不是项目成员"
        );

        return achievement;
    }

    /**
     * 批量验证任务是否存在且属于同一项目
     * 优化：一次性查询所有任务，减少数据库访问
     *
     * @param taskIds   任务ID列表
     * @param projectId 项目ID
     * @return 任务列表
     * @throws ServiceException 任务不存在、数量不匹配或不属于同一项目时抛出
     */
    private List<Task> validateTasksBatch(List<Long> taskIds, Long projectId) {
        // 批量查询任务（此方法需要在 TaskRepository 中实现自定义查询）
        List<Task> tasks = taskRepository.findByProjectIdAndIsDeleted(projectId, false);

        // 只保留本次需要关联的任务
        tasks = tasks.stream()
                .filter(task -> taskIds.contains(task.getId()))
                .toList();

        // 验证任务数量
        if (tasks.size() != taskIds.size()) {
            throw new ServiceException("部分任务不存在或不属于该项目");
        }

        // 验证所有任务都属于同一项目且未删除
        for (Task task : tasks) {
            if (!task.getProjectId().equals(projectId)) {
                throw new ServiceException("任务与成果不属于同一项目，无法关联");
            }
            if (task.getIsDeleted() != null && task.getIsDeleted()) {
                throw new ServiceException("任务已删除，无法关联");
            }
        }

        return tasks;
    }

    /**
     * 获取已存在的关联关系的任务ID集合
     * 优化：批量查询已存在的关联，避免循环查询
     *
     * @param achievementId 成果ID
     * @param taskIds       任务ID列表
     * @return 已关联的任务ID集合
     */
    private Set<Long> getExistingLinkedTaskIds(Long achievementId, List<Long> taskIds) {
        List<AchievementTaskRef> existingRefs =
                achievementTaskRefRepository.findByAchievementId(achievementId);

        Set<Long> existingTaskIds = existingRefs.stream()
                .map(AchievementTaskRef::getTaskId)
                .collect(Collectors.toSet());

        // 只返回本次需要关联的任务中已存在的
        return taskIds.stream()
                .filter(existingTaskIds::contains)
                .collect(Collectors.toSet());
    }

    /**
     * 批量删除任务关联关系
     * 优化：避免循环删除，提高性能
     *
     * @param achievementId 成果ID
     * @param taskIds       任务ID列表
     * @return 删除的记录数
     */
    private int batchDeleteTaskLinks(Long achievementId, List<Long> taskIds) {
        int deletedCount = 0;
        for (Long taskId : taskIds) {
            try {
                achievementTaskRefRepository.deleteByAchievementIdAndTaskId(achievementId, taskId);
                deletedCount++;
            } catch (Exception e) {
                log.warn("删除关联关系失败: achievementId={}, taskId={}", achievementId, taskId, e);
            }
        }
        return deletedCount;
    }

    /**
     * 批量获取任务的负责人映射
     * 优化：减少数据库查询次数
     *
     * @param taskIds 任务ID列表
     * @return Map<任务ID, 负责人列表>
     */
    private Map<Long, List<TaskUser>> getTaskUsersMapBatch(List<Long> taskIds) {
        Map<Long, List<TaskUser>> taskUsersMap = new HashMap<>();

        for (Long taskId : taskIds) {
            List<TaskUser> taskUsers = taskUserRepository.findActiveExecutorsByTaskId(taskId);
            taskUsersMap.put(taskId, taskUsers);
        }

        return taskUsersMap;
    }

    /**
     * 批量获取任务的最新提交信息映射
     * 优化：减少重复查询
     *
     * @param taskIds 任务ID列表
     * @return Map<任务ID, 最新提交>
     */
    private Map<Long, TaskSubmissionDTO> getLatestSubmissionsMapBatch(List<Long> taskIds) {
        Map<Long, TaskSubmissionDTO> submissionsMap = new HashMap<>();

        for (Long taskId : taskIds) {
            try {
                TaskSubmissionDTO submission = taskSubmissionService.getLatestSubmission(taskId);
                if (submission != null) {
                    submissionsMap.put(taskId, submission);
                }
            } catch (Exception e) {
                log.debug("获取任务最新提交失败: taskId={}", taskId, e);
            }
        }

        return submissionsMap;
    }

    /**
     * 将Task转换为TaskResultTaskRefDTO
     *
     * @param task            任务实体
     * @return 任务引用DTO
     */
    private TaskResultTaskRefDTO convertToTaskRefDTO(Task task) {
        if (task == null) {
            return null;
        }

        // 基础属性转换
        TaskResultTaskRefDTO dto = achievementConverter.toTaskResultTaskRefDTO(task);

        // 设置创建者名称
        dto.setCreatorName(userRepository.findNameById(task.getCreatorId()).orElse("未知用户"));

        // 处理负责人信息
        List<TaskUser> taskUsers = taskUserRepository.findActiveExecutorsByTaskId(task.getId());
        List<Long> assigneeIds = taskUsers.stream()
                .map(TaskUser::getUserId)
                .toList();
        dto.setAssigneeIds(JsonUtils.toJsonString(assigneeIds));

        // 设置负责人名称
        List<String> assigneeNames = assigneeIds.stream()
                .map(userId -> userRepository.findNameById(userId).orElse("未知用户"))
                .toList();
        dto.setAssigneeNames(assigneeNames);

        // 处理最新提交信息
        try {
            TaskSubmissionDTO submission = taskSubmissionService.getLatestSubmission(task.getId());
            if (submission != null) {
                dto.setLatestSubmissionId(parseLongSafely(submission.getId()));
                dto.setSubmitterId(parseLongSafely(submission.getSubmitterId()));
                dto.setSubmitterName(submission.getSubmitter() != null ? submission.getSubmitter().getName() : null);
                dto.setLatestSubmissionTime(submission.getSubmissionTime());
            }
        } catch (Exception e) {
            log.warn("获取任务提交信息失败: taskId={}", task.getId(), e);
        }

        // 设置完成时间
        if (task.getStatus() == TaskStatus.DONE) {
            dto.setCompletedAt(task.getUpdatedAt());
        }

        return dto;
    }

    /**
     * 安全解析Long类型，避免空指针和格式异常
     */
    private Long parseLongSafely(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
