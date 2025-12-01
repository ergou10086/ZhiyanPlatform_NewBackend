package hbnu.project.zhiyanbackend.knowledge.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementDetailDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementTemplateDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.UpdateDetailDataDTO;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementType;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementDetailsService;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.projects.utils.ProjectSecurityUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 成果详情管理接口
 * 负责成果详情的增删改查、模板管理等
 *
 * @author ErgouTree
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/zhiyan/achievement/detail")   // 未修改
@Tag(name = "成果详情管理", description = "成果详情编辑、模板管理、数据验证等")
public class AchievementDetailController {

    private final AchievementDetailsService achievementDetailsService;

    private final AchievementRepository achievementRepository;

    private final ProjectMemberService projectMemberService;

    private final ProjectSecurityUtils  projectSecurityUtils;


    /**
     * 更新成果详情数据
     * 更新成果的详细信息和摘要
     */
    @PutMapping("/details")
    @Operation(summary = "更新成果详情", description = "更新成果的详细信息JSON和摘要")
    public R<Void> updateAchievementDetails(
            @Valid @RequestBody UpdateDetailDataDTO updateDTO){
        Long userId = SecurityUtils.getUserId();
        log.info("更新成果详情: achievementId={}", updateDTO.getAchievementId());

        // 权限检查：必须有编辑权限（是项目成员且是成果创建者或项目管理员）
        projectSecurityUtils.checkEditPermission(updateDTO.getAchievementId(), userId);

        achievementDetailsService.updateDetailData(updateDTO);

        log.info("成果详情更新成功: achievementId={}", updateDTO.getAchievementId());
        return R.ok(null, "成果详情更新成功");
    }

    /**
     * 批量更新成果详情字段
     * 支持部分字段更新
     */
    @PatchMapping("/{achievementId}/fields")
    @Operation(summary = "批量更新详情字段", description = "部分更新成果的详情字段")
    public R<AchievementDetailDTO> updateDetailFields(
            @Parameter(description = "成果ID") @PathVariable Long achievementId,
            @RequestBody Map<String, Object> fieldUpdates) {
        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("批量更新详情字段: achievementId={}, fieldsCount={}, userId={}", 
                achievementId, fieldUpdates.size(), userId);

        // 权限检查：必须有编辑权限（项目成员且是创建者或管理员）
        projectSecurityUtils.checkEditPermission(achievementId, userId);

        AchievementDetailDTO result = achievementDetailsService.updateDetailFields(
                achievementId, fieldUpdates, userId
        );

        return R.ok(result, "字段更新成功");
    }

    /**
     * 更新成果摘要
     * 单独更新摘要信息
     */
    @PatchMapping("/{achievementId}/abstract")
    @Operation(summary = "更新成果摘要", description = "单独更新成果的摘要信息")
    public R<Void> updateAbstract(
            @Parameter(description = "成果ID") @PathVariable Long achievementId,
            @Parameter(description = "摘要内容") @RequestBody String abstractText) {
        Long userId = SecurityUtils.getUserId();
        log.info("更新成果摘要: achievementId={}", achievementId);

        // 权限检查：必须有编辑权限（项目成员且是创建者或管理员）
        projectSecurityUtils.checkEditPermission(achievementId, userId);

        achievementDetailsService.updateAbstract(achievementId, abstractText);

        return R.ok(null, "摘要更新成功");
    }

    /**
     * 创建自定义模板
     * 用户可以创建自己的成果字段模板
     */
    @PostMapping("/template/custom")
    @Operation(summary = "创建自定义模板", description = "创建用户自定义的成果字段模板")
    public R<AchievementTemplateDTO> createCustomTemplate(
            @Valid @RequestBody AchievementTemplateDTO templateDTO) {
        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("创建自定义模板: templateName={}, userId={}",
                templateDTO.getTemplateName(), userId);

        AchievementTemplateDTO result = achievementDetailsService.createCustomTemplate(templateDTO);

        log.info("自定义模板创建成功: templateId={}", result.getTemplateId());
        return R.ok(result, "自定义模板创建成功");
    }


    /**
     * 根据模板初始化成果详情
     * 为已存在的成果根据模板初始化详情数据
     */
    @PostMapping("/{achievementId}/initialize")
    @Operation(summary = "根据模板初始化详情", description = "为成果根据模板初始化详情数据")
    public R<AchievementDetailDTO> initializeTemplate(
            @Parameter(description = "成果ID") @PathVariable Long achievementId,
            @Parameter(description = "成果类型") @RequestParam AchievementType type,
            @RequestBody(required = false) Map<String, Object> initialData){
        log.info("根据模板初始化详情: achievementId={}, type={}",
                achievementId, type);

        AchievementDetailDTO result = achievementDetailsService.initializeDetailByTemplate(
                achievementId, type, initialData
        );

        return R.ok(result, "模板初始化成功");
    }


    /**
     * 验证成果详情数据
     * 验证详情数据是否符合模板要求
     */
    @PostMapping("/{achievementId}/validate")
    @Operation(summary = "验证成果详情数据", description = "验证详情数据是否符合模板要求")
    public R<Boolean> validateDetailData(
            @Parameter(description = "成果ID") @PathVariable Long achievementId,
            @RequestBody Map<String, Object> detailData) {
        log.info("验证成果详情数据: achievementId={}", achievementId);
        boolean isValid = achievementDetailsService.validateDetailData(achievementId, detailData);
        return R.ok(isValid, isValid ? "验证通过" : "验证失败");
    }
}
