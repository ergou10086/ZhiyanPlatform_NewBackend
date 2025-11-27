package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.model.entity.Permission;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.model.enums.SysRole;
import hbnu.project.zhiyanbackend.auth.model.enums.SystemPermission;
import hbnu.project.zhiyanbackend.auth.repository.PermissionRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.security.service.UserDetailsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证模块用户详情服务实现
 * 实现Spring Security的UserDetailsService接口，用于加载用户信息进行认证
 *
 * 功能说明：
 * 1. 根据邮箱加载用户信息（用于登录认证）
 * 2. 根据用户ID加载用户信息（用于业务查询）
 * 3. 加载用户的角色和权限信息
 * 4. 构建LoginUserBody对象供Spring Security使用
 * <p>
 * 优化说明：
 * - 新架构中User实体使用avatarData（BYTES）存储头像，需要转换为avatarUrl
 * - 保持与原架构相同的逻辑，确保认证流程一致性
 *
 * @author ErgouTree
 */
@Slf4j
@Service("authUserDetailsServiceImpl")
@RequiredArgsConstructor
public class AuthUserDetailsServiceImpl extends UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 根据邮箱加载用户信息
     * Spring Security在认证时会调用此方法
     *
     * @param email 用户邮箱（登录账号）
     * @return 用户详情对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("开始加载用户信息: {}", email);

        // 1. 查找用户基本信息
        Optional<User> optionalUser = userRepository.findByEmailAndIsDeletedFalse(email);
        if (optionalUser.isEmpty()) {
            log.warn("用户不存在: {}", email);
            throw new UsernameNotFoundException("用户不存在: " + email);
        }

        User user = optionalUser.get();

        // 2. 从数据库加载用户角色和权限
        Set<String> permissions = loadUserPermissionsFromDatabase(user.getId());
        List<String> roles = loadUserRolesFromDatabase(user.getId());

        log.debug("用户[{}]加载完成 - 角色数: {}, 权限数: {}",
                email, roles.size(), permissions.size());

        // 3. 构建头像URL（从avatarData转换为Base64 Data URL）
        String avatarUrl = buildAvatarUrl(user);

        // 4. 使用父类方法构建LoginUserBody对象
        return buildLoginUserBody(
                user.getId(),
                user.getEmail(),
                user.getName(),
                avatarUrl,
                user.getAvatarData(), // 添加头像二进制数据
                user.getAvatarContentType(), // 添加头像内容类型
                user.getTitle(),
                user.getInstitution(),
                roles,
                permissions,
                user.getIsLocked(),
                user.getPasswordHash()
        );
    }

    /**
     * 根据用户ID加载用户详情（业务特定方法）
     * 用于在业务代码中根据用户ID获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        Optional<User> optionalUser = userRepository.findByIdAndIsDeletedFalse(userId);
        if (optionalUser.isEmpty()) {
            log.warn("用户不存在: {}", userId);
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        User user = optionalUser.get();

        // 从数据库加载角色和权限
        Set<String> permissions = loadUserPermissionsFromDatabase(userId);
        List<String> roles = loadUserRolesFromDatabase(userId);

        // 构建头像URL
        String avatarUrl = buildAvatarUrl(user);

        return buildLoginUserBody(
                user.getId(),
                user.getEmail(),
                user.getName(),
                avatarUrl,
                user.getAvatarData(), // 添加头像二进制数据
                user.getAvatarContentType(), // 添加头像内容类型
                user.getTitle(),
                user.getInstitution(),
                roles,
                permissions,
                user.getIsLocked(),
                user.getPasswordHash()
        );
    }

    /**
     * 从数据库加载用户权限
     * 通过用户的角色关联查询所有权限
     *
     * 优化说明：
     * - 原架构逻辑保持不变，通过角色-权限关联查询
     * - 使用Set去重，确保权限唯一性
     *
     * @param userId 用户ID
     * @return 用户权限名称集合
     */
    private Set<String> loadUserPermissionsFromDatabase(Long userId) {
        try {
            // 1. 先根据用户ID加载其系统角色名称列表（例如 USER、DEVELOPER 等）
            List<String> roleNames = loadUserRolesFromDatabase(userId);

            // 2. 将角色名称映射到 SysRole 枚举，并汇总各角色对应的 SystemPermission
            Set<String> permissionCodes = roleNames.stream()
                    .map(roleName -> {
                        try {
                            return SysRole.valueOf(roleName);
                        } catch (IllegalArgumentException ex) {
                            // 数据库中的角色名在 SysRole 中没有对应枚举时，跳过并记录日志
                            log.warn("加载用户权限时发现未知系统角色: roleName={}, userId={}", roleName, userId);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .flatMap(sysRole -> sysRole.getPermissions().stream())
                    .map(SystemPermission::getCode)
                    .collect(Collectors.toSet());

            log.debug("用户[ID: {}]权限加载完成（基于SysRole枚举），共{}个权限", userId, permissionCodes.size());
            return permissionCodes;

        } catch (Exception e) {
            log.error("加载用户权限异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * 从数据库加载用户角色
     * 查询用户的所有角色关联
     * <p>
     * 优化说明：
     * - 原架构逻辑保持不变，通过UserRole关联查询
     * - 使用distinct去重，确保角色唯一性
     *
     * @param userId 用户ID
     * @return 用户角色名称列表
     */
    private List<String> loadUserRolesFromDatabase(Long userId) {
        try {
            Optional<User> optionalUser = userRepository.findByIdWithRolesAndPermissions(userId);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在 - userId: {}", userId);
                return Collections.emptyList();
            }

            User user = optionalUser.get();
            List<String> roles = user.getUserRoles().stream()
                    .map(ur -> ur.getRole().getName())
                    .distinct()
                    .collect(Collectors.toList());

            log.debug("用户[ID: {}]角色加载完成: {}", userId, roles);
            return roles;

        } catch (Exception e) {
            log.error("加载用户角色异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建头像URL
     * 新架构优化：从PostgreSQL BYTES字段转换为Base64 Data URL
     * <p>
     * 优化说明：
     * - 原架构使用MinIO存储，直接返回URL
     * - 新架构使用PostgreSQL BYTES存储，需要转换为Base64 Data URL
     * - 格式：data:image/jpeg;base64,/9j/4AAQSkZJRg...
     *
     * @param user 用户实体
     * @return 头像URL（Base64 Data URL格式），如果没有头像则返回null
     */
    private String buildAvatarUrl(User user) {
        if (user.getAvatarData() == null || user.getAvatarData().length == 0) {
            return null;
        }

        try {
            // 将BYTES数据转换为Base64字符串
            String base64 = Base64.getEncoder().encodeToString(user.getAvatarData());

            // 构建Data URL格式
            String contentType = user.getAvatarContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "image/jpeg"; // 默认类型
            }

            return "data:" + contentType + ";base64," + base64;
        } catch (Exception e) {
            log.warn("构建头像URL失败 - userId: {}, 错误: {}", user.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 获取用户权限（业务特定方法）
     * 供其他服务类调用，获取指定用户的权限集合
     *
     * @param userId 用户ID
     * @return 用户权限名称集合
     */
    public Set<String> getUserPermissions(Long userId) {
        return loadUserPermissionsFromDatabase(userId);
    }

    /**
     * 获取用户角色（业务特定方法）
     * 供其他服务类调用，获取指定用户的角色列表
     *
     * @param userId 用户ID
     * @return 用户角色名称列表
     */
    public List<String> getUserRoles(Long userId) {
        return loadUserRolesFromDatabase(userId);
    }
}