package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.knowledge.model.dto.TaskResultTaskRefDTO;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementTaskService;
import hbnu.project.zhiyanbackend.projects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkTasksToAchievement(Long achievementId, List<Long> taskIds, Long userId) {
        log.info("关联任务到成果: achievementId={}, taskIds={}, userId={}", achievementId, taskIds, userId);

        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("任务ID列表为空，跳过关联");
            return;
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
        return List.of();
    }

    /**
     * 获取任务关联的成果ID列表
     *
     * @param taskId 任务ID
     * @return 成果ID列表
     */
    @Override
    public List<Long> getLinkedAchievements(Long taskId) {
        return List.of();
    }

    /**
     * 批量获取任务关联的成果ID列表
     *
     * @param taskIds 任务ID列表
     * @return Map<任务ID, 成果ID列表>
     */
    @Override
    public Map<Long, List<Long>> getLinkedAchievementsBatch(List<Long> taskIds) {
        return Map.of();
    }
}
