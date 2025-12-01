package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.knowledge.model.dto.TaskResultTaskRefDTO;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementTaskRef;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementTaskRefRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementTaskService;
import hbnu.project.zhiyanbackend.projects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成果-任务关联服务实现
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementTaskServiceImpl implements AchievementTaskService {

    private final ProjectService projectService;
    private final AchievementTaskRefRepository achievementTaskRefRepository;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkTasksToAchievement(Long achievementId, List<Long> taskIds, Long userId) {
        log.info("关联任务到成果: achievementId={}, taskIds={}, userId={}", achievementId, taskIds, userId);

        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("任务ID列表为空，跳过关联");
            return;
        }
        // 去重
        List<Long> distinctTaskIds = taskIds.stream().distinct().toList();

        for (Long taskId : distinctTaskIds) {
            if (taskId == null) {
                continue;
            }

            // 已存在则跳过，避免重复关联
            boolean exists = achievementTaskRefRepository
                    .findByAchievementIdAndTaskId(achievementId, taskId)
                    .isPresent();
            if (exists) {
                continue;
            }

            AchievementTaskRef ref = AchievementTaskRef.builder()
                    .achievementId(achievementId)
                    .taskId(taskId)
                    .build();
            achievementTaskRefRepository.save(ref);
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
    public void unlinkTasksFromAchievement(Long achievementId, List<Long> taskIds, Long userId) {
        log.info("取消成果与任务的关联: achievementId={}, taskIds={}, userId={}", achievementId, taskIds, userId);

        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("任务ID列表为空，跳过取消关联");
            return;
        }

        for (Long taskId : taskIds) {
            if (taskId == null) {
                continue;
            }
            achievementTaskRefRepository.deleteByAchievementIdAndTaskId(achievementId, taskId);
        }
    }

    /**
     * 获取成果关联的任务列表（带详细信息）
     * 通过调用项目服务API获取任务详情
     *
     * @param achievementId 成果ID
     * @return 任务列表（包含任务详情）
     */
    @Override
    public List<TaskResultTaskRefDTO> getLinkedTasks(Long achievementId) {
        List<AchievementTaskRef> refs = achievementTaskRefRepository.findByAchievementId(achievementId);
        if (refs.isEmpty()) {
            return List.of();
        }

        List<TaskResultTaskRefDTO> results = new ArrayList<>();
        for (AchievementTaskRef ref : refs) {
            TaskResultTaskRefDTO dto = TaskResultTaskRefDTO.builder()
                    .id(ref.getTaskId())
                    .build();
            results.add(dto);
        }
        return results;
    }

    /**
     * 获取任务关联的成果ID列表
     *
     * @param taskId 任务ID
     * @return 成果ID列表
     */
    @Override
    public List<Long> getLinkedAchievements(Long taskId) {
        List<AchievementTaskRef> refs = achievementTaskRefRepository.findByTaskId(taskId);
        if (refs.isEmpty()) {
            return List.of();
        }
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
        Map<Long, List<Long>> result = new HashMap<>();
        if (taskIds == null || taskIds.isEmpty()) {
            return result;
        }

        for (Long taskId : taskIds) {
            if (taskId == null) {
                continue;
            }
            List<Long> achievements = getLinkedAchievements(taskId);
            if (!achievements.isEmpty()) {
                result.put(taskId, achievements);
            }
        }
        return result;
    }
}
