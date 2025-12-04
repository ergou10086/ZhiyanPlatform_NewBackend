package hbnu.project.zhiyanbackend.projects.controller;

import hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog;
import hbnu.project.zhiyanbackend.activelog.core.OperationLogHelper;
import hbnu.project.zhiyanbackend.activelog.model.enums.BizOperationModule;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.dto.ProjectMemberDetailDTO;
import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.repository.ProjectMemberRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 项目成员控制器
 *
 * @author Tokito
 */
@RestController
@RequestMapping("/zhiyan/projects")
@Tag(name = "项目成员管理(精简版)", description = "项目成员增删改查相关接口（无鉴权、无外部服务，只做基础成员管理）")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    private final ProjectMemberRepository projectMemberRepository;

    private final OperationLogHelper operationLogHelper;

    /**
     * 邀请/添加成员到项目（简化版：直接传 userId 和 role）
     */
    @PostMapping("/{projectId}/members")
    @Operation(summary = "添加项目成员", description = "直接为项目添加成员，不做权限和外部用户校验")
    @BizOperationLog(module = BizOperationModule.PROJECT, type = "MEMBER_ADD", description = "添加成员")
    public R<Void> addMember(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @RequestParam("userId") @Parameter(description = "用户ID") Long userId,
            @RequestParam("role") @Parameter(description = "成员角色", example = "MEMBER") ProjectMemberRole role
    ) {
        Long operatorId = SecurityUtils.getUserId();
        if (operatorId == null) {
            return R.fail("未登录或Token无效，无法添加成员");
        }
        if (!projectMemberService.isAdmin(projectId, operatorId) && !operatorId.equals(userId)) {
            return R.fail("只有项目管理员可以添加其他成员");
        }

        // 执行添加成员操作
        R<Void> result = projectMemberService.addMember(projectId, userId, role);

        // 操作成功后记录日志
        if (R.isSuccess(result)) {
            operationLogHelper.logMemberAdd(projectId, userId, role);
        }

        return result;
    }

    /**
     * 移除项目成员
     */
    @DeleteMapping("/{projectId}/members/{userId}")
    @Operation(summary = "移除项目成员", description = "从项目中移除指定用户")
    public R<Void> removeMember(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @PathVariable("userId") @Parameter(description = "用户ID") Long userId
    ) {
        Long operatorId = SecurityUtils.getUserId();
        if (operatorId == null) {
            return R.fail("未登录或Token无效，无法移除成员");
        }
        // 执行移除成员操作
        R<Void> result = projectMemberService.removeMember(projectId, userId, operatorId);

        // 操作成功后记录日志
        if (R.isSuccess(result)) {
            operationLogHelper.logMemberRemove(projectId, userId);
        }

        return result;
    }

    /**
     * 更新成员角色
     */
    @PutMapping("/{projectId}/members/{userId}/role")
    @Operation(summary = "更新成员角色", description = "修改项目成员在项目中的角色")
    public R<Void> updateMemberRole(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @PathVariable("userId") @Parameter(description = "用户ID") Long userId,
            @RequestParam("newRole") @Parameter(description = "新角色", example = "ADMIN") ProjectMemberRole newRole
    ) {
        Long operatorId = SecurityUtils.getUserId();
        if (operatorId == null) {
            return R.fail("未登录或Token无效，无法更新成员角色");
        }

        // 获取旧角色（用于日志记录）
        ProjectMemberRole oldRole = null;
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
        if (memberOpt.isPresent()) {
            oldRole = memberOpt.get().getProjectRole();
        }

        // 执行角色更新操作
        R<Void> result = projectMemberService.updateMemberRole(projectId, userId, newRole, operatorId);

        // 操作成功后记录日志
        if (R.isSuccess(result)) {
            operationLogHelper.logRoleChange(projectId, userId, oldRole, newRole);
        }

        return result;
    }

    /**
     * 获取项目成员分页列表（包含用户名称）
     */
    @GetMapping("/{projectId}/members")
    @Operation(summary = "获取项目成员列表", description = "分页获取指定项目的成员列表，包含用户详细信息")
    public R<Page<ProjectMemberDetailDTO>> getProjectMembers(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        // 使用ProjectMemberServiceImpl的getProjectMembersWithDetails方法，返回包含用户名称的DTO
        Page<ProjectMemberDetailDTO> members = ((hbnu.project.zhiyanbackend.projects.service.impl.ProjectMemberServiceImpl) projectMemberService)
                .getProjectMembersWithDetails(projectId, pageable);
        return R.ok(members);
    }

    /**
     * 获取指定角色成员列表（不分页）
     */
    @GetMapping("/{projectId}/members/role/{role}")
    @Operation(summary = "按角色获取成员", description = "获取项目中指定角色的成员列表")
    public R<List<ProjectMember>> getMembersByRole(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @PathVariable("role") @Parameter(description = "成员角色") ProjectMemberRole role
    ) {
        List<ProjectMember> members = projectMemberService.getMembersByRole(projectId, role);
        return R.ok(members);
    }

    /**
     * 检查指定用户是否为项目成员（供其他服务或前端使用）
     */
    @GetMapping("/{projectId}/members/check")
    @Operation(summary = "检查成员关系", description = "检查用户是否为项目成员")
    public R<Boolean> isProjectMember(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @RequestParam("userId") @Parameter(description = "用户ID") Long userId
    ) {
        boolean isMember = projectMemberService.isMember(projectId, userId);
        return R.ok(isMember);
    }

    /**
     * 检查当前登录用户是否为项目拥有者
     */
    @GetMapping("/{projectId}/check-owner")
    @Operation(summary = "检查是否为项目拥有者", description = "检查当前登录用户在项目中是否为 OWNER")
    public R<Boolean> isProjectOwner(
            @PathVariable("projectId") Long projectId
    ) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法检查项目拥有者身份");
        }
        boolean isOwner = projectMemberService.isOwner(projectId, userId);
        return R.ok(isOwner);
    }

    /**
     * 检查当前登录用户是否为项目管理员（OWNER 或 ADMIN）
     */
    @GetMapping("/{projectId}/check-admin")
    @Operation(summary = "检查是否为项目管理员", description = "检查当前登录用户在项目中是否为管理员（包含 OWNER 和 ADMIN）")
    public R<Boolean> isProjectAdmin(
            @PathVariable("projectId") Long projectId
    ) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法检查项目管理员权限");
        }
        boolean isAdmin = projectMemberService.isAdmin(projectId, userId);
        return R.ok(isAdmin);
    }

    /**
     * 获取项目成员数量
     */
    @GetMapping("/{projectId}/members/count")
    @Operation(summary = "获取项目成员数量", description = "返回某个项目的成员总数")
    public R<Long> getMemberCount(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId
    ) {
        long count = projectMemberService.getMemberCount(projectId);
        return R.ok(count);
    }

    /**
     * 获取项目成员用户ID列表（内部使用）
     */
    @GetMapping("/{projectId}/members/user-ids")
    @Operation(summary = "获取项目成员用户ID列表", description = "返回指定项目下所有成员的用户ID列表")
    public R<List<Long>> getProjectMemberUserIds(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId
    ) {
        List<Long> userIds = projectMemberService.getProjectMemberUserIds(projectId);
        return R.ok(userIds);
    }
}
