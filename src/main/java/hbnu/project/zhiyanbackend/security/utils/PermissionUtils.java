package hbnu.project.zhiyanbackend.security.utils;


import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.security.context.LoginUserBody;
import hbnu.project.zhiyanbackend.security.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 权限工具类
 * 提供系统级和项目级的权限检查方法
 *
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static ProjectMemberService projectMemberService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        PermissionUtils.applicationContext = applicationContext;
        PermissionUtils.projectMemberService = applicationContext.getBean(ProjectMemberService.class);
    }

    // ==================== 系统级权限检查 ====================

    /**
     * 检查当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        LoginUserBody loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        return loginUser.hasPermission(permission);
    }

    /**
     * 检查当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        LoginUserBody loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        return loginUser.hasRole(role);
    }

    /**
     * 检查当前用户是否拥有任意一个指定权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        LoginUserBody loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        return loginUser.hasAnyPermission(permissions);
    }

    /**
     * 检查当前用户是否拥有任意一个指定角色
     */
    public static boolean hasAnyRole(String... roles) {
        LoginUserBody loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        return loginUser.hasAnyRole(roles);
    }

    /**
     * 检查当前用户是否拥有所有指定权限
     */
    public static boolean hasAllSpecifiedPermission(String... permissions) {
        LoginUserBody loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null || loginUser.getPermissions() == null) {
            return false;
        }
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    // ==================== 系统级角色检查 ====================

    /**
     * 检查当前用户是否可以创建项目
     */
    public static boolean canCreateProject() {
        return hasPermission("project:create");
    }

    /**
     * 检查当前用户是否可以管理项目（基于项目ID的动态权限检查）
     * 注意：这里只检查基础权限，实际项目管理权限需要结合项目成员关系判断
     */
    public static boolean canManageProject() {
        return hasPermission("project:manage");
    }

    /**
     * 检查当前用户是否可以删除项目
     * 注意：实际删除权限需要结合项目所有者关系判断
     */
    public static boolean canDeleteProject() {
        return hasPermission("project:delete");
    }

    /**
     * 检查当前用户是否可以管理知识库
     */
    public static boolean canManageKnowledge() {
        return hasPermission("knowledge:manage");
    }

    /**
     * 检查当前用户是否可以管理Wiki
     */
    public static boolean canManageWiki() {
        return hasPermission("wiki:manage");
    }

    /**
     * 检查当前用户是否可以查看Wiki
     */
    public static boolean canViewWiki() {
        return hasPermission("wiki:view");
    }

    /**
     * 检查当前用户是否可以编辑Wiki
     */
    public static boolean canEditWiki() {
        return hasPermission("wiki:edit");
    }

    /**
     * 检查当前用户是否可以创建Wiki
     */
    public static boolean canCreateWiki() {
        return hasPermission("wiki:create");
    }

    /**
     * 检查当前用户是否可以删除Wiki
     */
    public static boolean canDeleteWiki() {
        return hasPermission("wiki:delete");
    }

    /**
     * 检查当前用户是否可以管理任务
     */
    public static boolean canManageTask() {
        return hasPermission("task:manage");
    }

    /**
     * 检查当前用户是否可以创建任务
     */
    public static boolean canCreateTask() {
        return hasPermission("task:create");
    }

    /**
     * 检查当前用户是否可以管理项目成员
     */
    public static boolean canManageMember() {
        return hasPermission("member:manage");
    }

    /**
     * 检查当前用户是否为系统管理员
     */
    public static boolean isSystemAdmin() {
        return hasRole("DEVELOPER") || hasPermission("system:admin");
    }

    /**
     * 检查当前用户是否为开发者
     */
    public static boolean isDeveloper() {
        return hasRole("DEVELOPER");
    }

    /**
     * 检查当前用户是否为普通用户
     */
    public static boolean isUser() {
        return hasRole("USER");
    }

    // ==================== 用户信息获取 ====================

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        LoginUserBody loginUser = SecurityContextHolder.getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * 获取当前用户邮箱
     */
    public static String getCurrentUserEmail() {
        LoginUserBody loginUser = SecurityContextHolder.getLoginUser();
        return loginUser != null ? loginUser.getEmail() : null;
    }

    /**
     * 检查当前用户是否已登录
     */
    public static boolean isLoggedIn() {
        return SecurityContextHolder.isLogin();
    }

    // ==================== 项目级权限检查 ====================

    /**
     * 检查当前用户在指定项目中是否具有指定角色
     */
    public static boolean hasProjectRole(Long projectId, String projectRole) {
        ValidationUtils.requireNonNull(projectId, "projectId不能为空");
        ValidationUtils.requireNonNull(projectRole, "projectRole不能为空");

        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        try {
            ProjectMemberRole role = getProjectMemberService().getUserRole(projectId, userId);
        }catch (Exception e) {
            log.error("检查项目角色失败: projectId={}, userId={}, role={}",
                    projectId, userId, projectRole, e);
            return false;
        }
        log.debug("检查用户在项目[{}]中是否具有角色[{}]，当前未实现", projectId, projectRole);
        return false;
    }

    /**
     * 检查当前用户在指定项目中是否具有指定角色（枚举类型）
     */
    public static boolean hasProjectRole(Long projectId, ProjectMemberRole projectRole) {
        if (projectId == null || projectRole == null) {
            return false;
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        try {
            ProjectMemberRole userRole = getProjectMemberService().getUserRole(projectId, userId);
            return projectRole.equals(userRole);
        } catch (Exception e) {
            log.error("检查项目角色失败: projectId={}, userId={}, role={}",
                    projectId, userId, projectRole, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否为指定项目的拥有者
     */
    public static boolean isProjectOwner(Long projectId) {
        return hasProjectRole(projectId, "OWNER");
    }

    /**
     * 检查当前用户是否为指定项目的成员
     */
    public static boolean isProjectMember(Long projectId) {
        if (projectId == null) {
            return false;
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        try {
            return getProjectMemberService().isMember(projectId, userId);
        } catch (Exception e) {
            log.error("检查项目成员身份失败: projectId={}, userId={}", projectId, userId, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否为指定项目的管理员（包括OWNER和ADMIN）
     */
    public static boolean isProjectAdmin(Long projectId) {
        if (projectId == null) {
            return false;
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        try {
            return getProjectMemberService().isAdmin(projectId, userId);
        } catch (Exception e) {
            log.error("检查项目管理员身份失败: projectId={}, userId={}", projectId, userId, e);
            return false;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取ProjectMemberService实例（用于静态方法调用）
     */
    private static ProjectMemberService getProjectMemberService() {
        if (projectMemberService == null && applicationContext != null) {
            try {
                projectMemberService = applicationContext.getBean(ProjectMemberService.class);
            } catch (Exception e) {
                log.error("获取ProjectMemberService失败", e);
            }
        }
        if (projectMemberService == null) {
            throw new IllegalStateException("ProjectMemberService未初始化，请确保PermissionUtils已正确配置为Spring Bean");
        }
        return projectMemberService;
    }
}