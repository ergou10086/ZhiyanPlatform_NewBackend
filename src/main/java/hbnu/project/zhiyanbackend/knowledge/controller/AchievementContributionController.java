package hbnu.project.zhiyanbackend.knowledge.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementContributionDTO;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementContributionService;
import hbnu.project.zhiyanbackend.projects.utils.ProjectSecurityUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 成果贡献统计接口
 * 提供贡献热力图所需的数据
 *
 * @author ErgouTree
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/zhiyan/achievement/contribution")
@Tag(name = "成果贡献统计", description = "成果贡献热力图数据统计")
public class AchievementContributionController {

    private final AchievementContributionService contributionService;
    private final ProjectSecurityUtils projectSecurityUtils;

    /**
     * 获取项目成果贡献统计数据
     * 用于生成贡献热力图
     *
     * @param projectId 项目ID
     * @param startDate 开始日期（可选，默认一年前）
     * @param endDate   结束日期（可选，默认今天）
     * @return 贡献统计数据列表
     */
    @GetMapping("/{projectId}")
    @Operation(summary = "获取项目成果贡献统计", description = "获取指定时间范围内的成果贡献统计数据，用于生成热力图")
    public R<List<AchievementContributionDTO>> getContributions(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "开始日期（格式：yyyy-MM-dd）") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期（格式：yyyy-MM-dd）") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long userId = SecurityUtils.getUserId();
        log.info("获取项目成果贡献统计: projectId={}, userId={}, startDate={}, endDate={}", 
                projectId, userId, startDate, endDate);

        // 权限检查：必须是项目成员
        projectSecurityUtils.isMember(projectId, userId);

        // 默认查询最近一年
        if (startDate == null || endDate == null) {
            List<AchievementContributionDTO> contributions = contributionService.getContributionsLastYear(projectId);
            return R.ok(contributions, "查询成功");
        }

        // 查询指定日期范围
        List<AchievementContributionDTO> contributions = contributionService.getContributions(projectId, startDate, endDate);
        return R.ok(contributions, "查询成功");
    }
}

