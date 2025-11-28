package hbnu.project.zhiyanbackend.projects.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目成员控制器
 * 项目成员采用直接邀请方式，无需申请审批流程
 *
 * @author Tokito
 */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "项目成员管理(精简版)", description = "项目成员增删改查相关接口（无鉴权、无外部服务，只做基础成员管理）")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    /**
     * 邀请/添加成员到项目（简化版：直接传 userId 和 role）
     */
    @PostMapping("/{projectId}/members")
    @Operation(summary = "添加项目成员", description = "直接为项目添加成员，不做权限和外部用户校验")
    public R<Void> addMember(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @RequestParam("userId") @Parameter(description = "用户ID") Long userId,
            @RequestParam("role") @Parameter(description = "成员角色", example = "MEMBER") ProjectMemberRole role
    ) {
        return projectMemberService.addMember(projectId, userId, role);
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
        return projectMemberService.removeMember(projectId, userId, operatorId);
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
        return projectMemberService.updateMemberRole(projectId, userId, newRole, operatorId);
    }

    /**
     * 获取项目成员分页列表
     */
    @GetMapping("/{projectId}/members")
    @Operation(summary = "获取项目成员列表", description = "分页获取指定项目的成员列表")
    public R<Page<ProjectMember>> getProjectMembers(
            @PathVariable("projectId") @Parameter(description = "项目ID") Long projectId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProjectMember> members = projectMemberService.getProjectMembers(projectId, pageable);
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
     * 检查用户是否为项目拥有者
     */
    @GetMapping("/{projectId}/owner/check")
    @Operation(summary = "检查是否为项目拥有者", description = "检查用户在项目中是否为 OWNER")
    public ResponseEntity<Boolean> isProjectOwner(
            @PathVariable("projectId") Long projectId,
            @RequestParam("userId") Long userId
    ) {
        boolean isOwner = projectMemberService.isOwner(projectId, userId);
        return new ResponseEntity<>(isOwner, HttpStatus.OK);
    }

    /**
     * 检查用户是否为项目管理员（OWNER 或 ADMIN）
     */
    @GetMapping("/{projectId}/admin/check")
    @Operation(summary = "检查是否为项目管理员", description = "检查用户在项目中是否为管理员（包含 OWNER 和 ADMIN）")
    public ResponseEntity<Boolean> isProjectAdmin(
            @PathVariable("projectId") Long projectId,
            @RequestParam("userId") Long userId
    ) {
        boolean isAdmin = projectMemberService.isAdmin(projectId, userId);
        return new ResponseEntity<>(isAdmin, HttpStatus.OK);
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
