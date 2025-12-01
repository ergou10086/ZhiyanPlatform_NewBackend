package hbnu.project.zhiyanbackend.knowledge.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementDetailDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.CreateAchievementDTO;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementDetailsService;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementFileService;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementService;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementTaskService;
import hbnu.project.zhiyanbackend.projects.utils.ProjectSecurityUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成果管理接口
 * 能够创建成果，并且编写对应的详情，给成果内的文件管理做基础
 *
 * @author ErgouTree
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/zhiyan/achievement")    // 未修改
@Tag(name = "成果管理", description = "成果创建、详情编辑，成果查询等管理成果的接口")
public class AchievementManageController {

    private final AchievementService achievementService;

    private final AchievementDetailsService achievementDetailsService;

    private final AchievementFileService achievementFileService;

    private final AchievementRepository achievementRepository;

    private final AchievementTaskService achievementTaskService;

    private final ProjectSecurityUtils projectSecurityUtils;

    /**
     * 创建成果
     * 创建一个新的成果，包含基本信息和详情数据
     */
    @PostMapping("/create")
    @Operation(summary = "创建成果", description = "为指定项目创建新的成果记录")
    public R<AchievementDTO> createAchievement(
            @Valid @RequestBody CreateAchievementDTO createDTO){
        log.info("创建成果请求: projectId={}, title={}, type={}",
                createDTO.getProjectId(), createDTO.getTitle(), createDTO.getType());

        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            createDTO.setCreatorId(userId);
        }

        // 权限检查：必须是项目成员
        projectSecurityUtils.isMember(createDTO.getProjectId(), userId);

        AchievementDTO result = achievementDetailsService.createAchievementWithDetails(createDTO);

        // 将字符串类型的成果ID转换为 Long，用于后续服务调用
        Long achievementId = null;
        if (result != null && result.getId() != null) {
            try {
                achievementId = Long.valueOf(result.getId());
            } catch (NumberFormatException e) {
                log.error("创建成果返回的ID无法转换为Long类型: id={}", result.getId(), e);
                throw new ServiceException("创建成果失败：成果ID格式不合法");
            }
        }

        // 如果创建时传入了关联任务ID列表，则建立成果-任务关联关系
        if (createDTO.getLinkedTaskIds() != null && !createDTO.getLinkedTaskIds().isEmpty()) {
            if (achievementId == null) {
                log.error("成果ID为空，无法建立任务关联: result={}", result);
                throw new ServiceException("创建成果失败：成果ID缺失");
            }
            log.info("为成果建立任务关联: achievementId={}, taskIds={}", achievementId, createDTO.getLinkedTaskIds());
            achievementTaskService.linkTasksToAchievement(achievementId, createDTO.getLinkedTaskIds(), userId);
        }

        log.info("成果创建成功: achievementId={}", result != null ? result.getId() : null);
        return R.ok(result, "成果创建成功");
    }

    /**
     * 更新成果状态
     * 修改成果的发布状态（草稿、已发布、归档等）
     */
    @PatchMapping("/{achievementId}/status")
    @Operation(summary = "更新成果状态", description = "修改成果的发布状态")
    public R<Void> updateAchievementStatus(
            @Parameter(description = "成果ID") @PathVariable Long achievementId,
            @Parameter(description = "新状态") @RequestParam AchievementStatus status){
        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("更新成果状态: achievementId={}, status={}, userId={}",
                achievementId, status, userId);

        projectSecurityUtils.checkEditPermission(achievementId, userId);

        achievementService.updateAchievementStatus(achievementId, status, userId);

        log.info("成果状态更新成功: achievementId={}, newStatus={}", achievementId, status);
        return R.ok(null, "状态更新成功");
    }

    /**
     * 获取成果详情
     * 查询成果的完整信息，包含详情数据
     */
    @GetMapping("/{achievementId}")
    @Operation(summary = "获取成果详情", description = "根据ID查询成果的完整信息")
    public R<AchievementDetailDTO>  getAchievementDetail(
            @Parameter(description = "成果ID") @PathVariable Long achievementId) {
        log.info("查询成果详情: achievementId={}", achievementId);

        // 权限检查：必须有访问权限
        projectSecurityUtils.requireAccess(achievementId);

        AchievementDetailDTO detail = achievementDetailsService.getAchievementDetail(achievementId);

        return R.ok(detail, "查询成功");
    }

    /**
     * 删除成果
     * 删除成果及其关联的所有数据（详情、文件等）
     */
    @DeleteMapping("/{achievementId}")
    @Operation(summary = "删除成果", description = "删除指定成果及其关联数据")
    public R<Void> deleteAchievement(
            @Parameter(description = "成果ID") @PathVariable Long achievementId){
        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("删除成果开始: achievementId={}, userId={}", achievementId, userId);

        // 删除成果需要为成果创建者和管理员,和编辑是一样的
        projectSecurityUtils.checkEditPermission(achievementId, userId);

        // 删除成果的文件
        List<AchievementFileDTO> files = achievementFileService.getFilesByAchievementId(achievementId);
        if (!files.isEmpty()) {
            List<Long> fileIds = files.stream()
                    .map(AchievementFileDTO::getId)
                    .toList();
            log.info("开始删除成果关联的文件: achievementId={}, fileCount={}", achievementId, fileIds.size());
            achievementFileService.deleteFiles(fileIds, userId);
            log.info("成果关联文件删除完成: achievementId={}", achievementId);
        }

        // 删除成果关联的详情数据
        log.info("开始删除成果详情: achievementId={}", achievementId);
        achievementDetailsService.deleteDetailByAchievementId(achievementId);
        log.info("成果详情删除完成: achievementId={}", achievementId);

        // 删除成果的数据库记录
        log.info("开始删除成果主表记录: achievementId={}", achievementId);
        achievementRepository.deleteById(achievementId);
        log.info("成果主表记录删除完成: achievementId={}", achievementId);

        log.info("成果删除全部完成: achievementId={}", achievementId);
        return R.ok(null, "成果删除成功");
    }

    /**
     * 更新成果公开性
     * 修改成果是否公开（需要编辑权限）
     */
    @PatchMapping("/{achievementId}/visibility")
    @Operation(summary = "更新成果的公开性", description = "修改成果的公开/私有状态")
    public R<Void> updateAchievementVisibility(
            @Parameter(description = "成果ID") @PathVariable Long achievementId,
            @Parameter(description = "公开性") @RequestParam Boolean isPublic){
        Long userId = SecurityUtils.getUserId();
        log.info("更新成果公开性: achievementId={}, isPublic={}, userId={}",
                achievementId, isPublic, userId);

        // 要有成果编辑的权限
        projectSecurityUtils.checkEditPermission(achievementId, userId);

        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ControllerException("成果不存在"));

        achievement.setIsPublic(isPublic);
        achievementRepository.save(achievement);

        log.info("成果公开性更新成功: achievementId={}, isPublic={}", achievementId, isPublic);
        return R.ok(null, "公开性更新成功");
    }
}
