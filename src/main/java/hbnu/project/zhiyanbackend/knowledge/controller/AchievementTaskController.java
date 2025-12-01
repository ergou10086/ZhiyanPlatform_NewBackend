package hbnu.project.zhiyanbackend.knowledge.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.knowledge.model.dto.TaskResultTaskRefDTO;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementTaskService;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成果-任务关联控制器
 * 提供成果与任务关联的RESTful API接口
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/achievement/{achievementId}/tasks")
@RequiredArgsConstructor
@Tag(name = "成果-任务关联", description = "管理成果与任务的关联关系")
public class AchievementTaskController {

    private final AchievementTaskService achievementTaskService;

    /**
     * 关联任务到成果
     *
     * @param achievementId 成果ID
     * @param taskIds 任务ID列表
     * @return 操作结果
     */
    @PostMapping("/link")
    @Operation(summary = "关联任务到成果", description = "将指定的任务关联到成果，支持批量关联")
    public R<Void> linkTasks(
            @Parameter(description = "成果ID") @PathVariable Long achievementId,
            @Parameter(description = "任务ID列表") @RequestBody List<Long> taskIds) {
        log.info("关联任务到成果: achievementId={}, taskIds={}", achievementId, taskIds);

        Long userId = SecurityUtils.getUserId();
        achievementTaskService.linkTasksToAchievement(achievementId, taskIds, userId);

        return R.ok(null, "任务关联成功");
    }


    /**
     * 取消关联任务
     *
     * @param achievementId 成果ID
     * @param taskIds 任务ID列表
     * @return 操作结果
     */
    @DeleteMapping("/unlink")
    @Operation(summary = "取消关联任务", description = "取消成果与指定任务的关联关系，支持批量取消")
    public R<Void> unlinkTasks(@Parameter(description = "成果ID") @PathVariable Long achievementId,
                               @Parameter(description = "任务ID列表") @RequestBody List<Long> taskIds) {
        log.info("取消关联任务: achievementId={}, taskIds={}", achievementId, taskIds);

        Long userId = SecurityUtils.getUserId();
        achievementTaskService.unlinkTasksFromAchievement(achievementId, taskIds, userId);

        return R.ok(null, "取消关联成功");
    }


    /**
     * 获取关联的任务列表
     *
     * @param achievementId 成果ID
     * @return 任务列表（包含任务详情）
     */
    @GetMapping
    @Operation(summary = "获取关联的任务列表", description = "查询成果关联的所有任务，包含任务详细信息")
    public R<List<TaskResultTaskRefDTO>> getLinkedTasks(
            @Parameter(description = "成果ID") @PathVariable Long achievementId) {
        log.info("获取关联的任务列表: achievementId={}", achievementId);

        List<TaskResultTaskRefDTO> tasks = achievementTaskService.getLinkedTasks(achievementId);

        return R.ok(tasks);
    }
}
