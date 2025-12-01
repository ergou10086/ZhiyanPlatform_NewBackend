package hbnu.project.zhiyanbackend.projects.utils;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectPermission;
import hbnu.project.zhiyanbackend.projects.repository.ProjectMemberRepository;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 项目级别的安全工具类
 * 用于检查用户在特定项目中的权限和成果的访问控制
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectSecurityUtils {

    private final ProjectMemberRepository projectMemberRepository;

    private final AchievementRepository achievementRepository;

    // ==================== 基础成员检查方法 ====================

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        return SecurityUtils.getUserId();
    }

    /**
     * 获取项目成员信息
     */
    private Optional<ProjectMember> getProjectMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
    }

    // ==================== 项目成员身份检查 ====================

    /**
     * 检查当前用户是否为项目成员
     */
    public boolean isMember(Long projectId) {
        return isMember(projectId, getCurrentUserId());
    }

    /**
     * 检查指定用户是否为项目成员
     */
    public boolean isMember(Long projectId, Long userId) {
        if (userId == null) {
            return false;
        }
        return getProjectMember(projectId, userId).isPresent();
    }

    /**
     * 检查当前用户是否为项目拥有者
     */
    public boolean isOwner(Long projectId) {
        return isOwner(projectId, getCurrentUserId());
    }

    /**
     * 检查指定用户是否为项目拥有者
     */
    public boolean isOwner(Long projectId, Long userId) {
        return hasRole(projectId, userId, ProjectMemberRole.OWNER);
    }

    /**
     * 检查当前用户是否为项目管理员（包括OWNER和ADMIN）
     */
    public boolean isAdmin(Long projectId) {
        return isAdmin(projectId, getCurrentUserId());
    }

    /**
     * 检查指定用户是否为项目管理员（包括OWNER和ADMIN）
     */
    public boolean isAdmin(Long projectId, Long userId) {
        if (userId == null) {
            return false;
        }

        Optional<ProjectMember> member = getProjectMember(projectId, userId);
        if (member.isEmpty()) {
            return false;
        }

        ProjectMemberRole role = member.get().getProjectRole();
        return role == ProjectMemberRole.OWNER || role == ProjectMemberRole.ADMIN;
    }

    /**
     * 检查用户是否拥有指定角色
     */
    private boolean hasRole(Long projectId, Long userId, ProjectMemberRole role) {
        if (userId == null) {
            return false;
        }

        Optional<ProjectMember> member = getProjectMember(projectId, userId);
        return member.isPresent() && member.get().getProjectRole() == role;
    }

    // ==================== 项目权限检查 ====================

    /**
     * 检查当前用户在项目中是否拥有指定权限
     */
    public boolean hasPermission(Long projectId, ProjectPermission permission) {
        return hasPermission(projectId, getCurrentUserId(), permission);
    }

    /**
     * 检查指定用户在项目中是否拥有指定权限
     */
    public boolean hasPermission(Long projectId, Long userId, ProjectPermission permission) {
        if (userId == null) {
            return false;
        }

        Optional<ProjectMember> member = getProjectMember(projectId, userId);
        return member.map(projectMember -> projectMember.getProjectRole().hasPermission(permission)).orElse(false);
    }

    // ==================== 角色获取方法 ====================

    /**
     * 获取当前用户在项目中的角色
     */
    public ProjectMemberRole getRole(Long projectId) {
        return getRole(projectId, getCurrentUserId());
    }

    /**
     * 获取指定用户在项目中的角色
     */
    public ProjectMemberRole getRole(Long projectId, Long userId) {
        if (userId == null) {
            return null;
        }

        Optional<ProjectMember> member = getProjectMember(projectId, userId);
        return member.map(ProjectMember::getProjectRole).orElse(null);
    }

    // ==================== 权限验证（抛出异常） ====================

    /**
     * 验证当前用户是否为项目成员，如果不是则抛出异常
     */
    public void requireMember(Long projectId) {
        if (!isMember(projectId)) {
            throw new ServiceException("您不是该项目的成员，无权访问");
        }
    }

    /**
     * 验证当前用户是否为项目拥有者，如果不是则抛出异常
     */
    public void requireOwner(Long projectId) {
        if (!isOwner(projectId)) {
            throw new ServiceException("只有项目拥有者可以执行此操作");
        }
    }

    /**
     * 验证当前用户是否为项目管理员，如果不是则抛出异常
     */
    public void requireAdmin(Long projectId) {
        if (!isAdmin(projectId)) {
            throw new ServiceException("只有项目管理员可以执行此操作");
        }
    }

    /**
     * 验证当前用户在项目中拥有指定权限，如果没有则抛出异常
     */
    public void requirePermission(Long projectId, ProjectPermission permission) {
        if (!hasPermission(projectId, permission)) {
            throw new ServiceException("您在该项目中没有 " + permission.getDescription() + " 权限");
        }
    }

    // ==================== 成果访问控制 ====================

    /**
     * 获取成果信息（内部辅助方法）
     */
    private Achievement getAchievement(Long achievementId) {
        return achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在"));
    }

    /**
     * 检查当前用户是否有权限访问成果
     */
    public boolean canAccess(Long achievementId) {
        return isProjectMember(achievementId);
    }

    /**
     * 检查用户是否是成果所属项目的成员
     */
    private boolean isProjectMember(Long achievementId) {
        Achievement achievement = getAchievement(achievementId);
        return isMember(achievement.getProjectId(), getCurrentUserId());
    }

    /**
     * 验证当前用户是否有成果的访问权限，如果没有则抛出异常
     */
    public void requireAccess(Long achievementId) {
        Achievement achievement = getAchievement(achievementId);

        // 公开成果直接允许访问
        if (Boolean.TRUE.equals(achievement.getIsPublic())) {
            return;
        }

        // 私有成果需要验证项目成员身份
        if (!isMember(achievement.getProjectId())) {
            throw new ServiceException("您没有权限访问此成果，该成果为项目私有，只有项目成员可以访问");
        }
    }

    /**
     * 检查用户是否有编辑成果的权限
     * 规则：必须是项目成员，且是成果创建者或项目管理员/拥有者
     */
    public void checkEditPermission(Long achievementId, Long userId) {
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }

        Achievement achievement = getAchievement(achievementId);
        Long projectId = achievement.getProjectId();

        // 验证是否为项目成员
        if (!isMember(projectId, userId)) {
            throw new ServiceException("无权限操作该成果，您不是项目成员");
        }

        // 检查权限条件
        boolean isCreator = achievement.getCreatorId().equals(userId);
        boolean isAdminOrOwner = isAdmin(projectId, userId) || isOwner(projectId, userId);

        if (!isCreator && !isAdminOrOwner) {
            throw new ServiceException("无权限编辑该成果，只有创建者、项目管理员或项目拥有者可以编辑");
        }
    }
}