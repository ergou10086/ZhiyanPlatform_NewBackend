package hbnu.project.zhiyanbackend.auth.service;

import hbnu.project.zhiyanbackend.auth.model.dto.PermissionDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.Permission;
import hbnu.project.zhiyanbackend.basic.domain.R;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 * 提供权限管理、权限校验等核心功能
 *
 * @author ErgouTree
 */
public interface PermissionService {

    /**
     * 检查用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permission 权限标识
     * @return 是否拥有权限
     */
    R<Boolean> hasPermission(Long userId, String permission);

    /**
     * 检查用户是否拥有指定权限列表中的任一权限
     *
     * @param userId 用户ID
     * @param permissions 权限列表
     * @return 是否拥有任一权限
     */
    R<Boolean> hasAnyPermission(Long userId, List<String> permissions);

    /**
     * 检查用户是否拥有所有指定权限
     *
     * @param userId 用户ID
     * @param permissions 权限列表
     * @return 是否拥有所有权限
     */
    R<Boolean> hasAllPermissions(Long userId, List<String> permissions);

    /**
     * 获取用户的所有权限
     *
     * @param userId 用户ID
     * @return 用户权限名称集合
     */
    R<Set<String>> getUserPermissions(Long userId);

    /**
     * 获取所有权限列表（分页）
     *
     * @param pageable 分页参数
     * @return 权限列表
     */
    R<Page<PermissionDTO>> getAllPermissions(Pageable pageable);

    /**
     * 根据ID获取权限详细信息
     *
     * @param permissionId 权限ID
     * @return 权限详细信息
     */
    R<PermissionDTO> getPermissionById(Long permissionId);

    /**
     * 根据名称查找权限
     *
     * @param permissionName 权限名称
     * @return 权限信息
     */
    R<PermissionDTO> getPermissionByName(String permissionName);

    /**
     * 根据ID查找权限实体（内部使用）
     *
     * @param permissionId 权限ID
     * @return 权限实体
     */
    Permission findById(Long permissionId);

    /**
     * 创建新权限
     *
     * @param permissionDTO 权限信息
     * @return 创建后的权限信息
     */
    R<PermissionDTO> createPermission(PermissionDTO permissionDTO);

    /**
     * 更新权限信息
     *
     * @param permissionId 权限ID
     * @param permissionDTO 更新的权限信息
     * @return 更新后的权限信息
     */
    R<PermissionDTO> updatePermission(Long permissionId, PermissionDTO permissionDTO);

    /**
     * 删除权限
     *
     * @param permissionId 权限ID
     * @return 删除结果
     */
    R<Void> deletePermission(Long permissionId);

    /**
     * 批量创建权限
     *
     * @param permissionDTOs 权限信息列表
     * @return 创建结果
     */
    R<List<PermissionDTO>> batchCreatePermissions(List<PermissionDTO> permissionDTOs);

    /**
     * 获取拥有指定权限的角色列表
     *
     * @param permissionId 权限ID
     * @param pageable 分页参数
     * @return 角色ID列表
     */
    R<Page<Long>> getPermissionRoles(Long permissionId, Pageable pageable);

    /**
     * 统计拥有指定权限的角色数量
     *
     * @param permissionId 权限ID
     * @return 角色数量
     */
    R<Long> countPermissionRoles(Long permissionId);

    /**
     * 初始化系统默认权限
     *
     * @return 初始化结果
     */
    R<Void> initializeSystemPermissions();

    /**
     * 检查系统权限是否已初始化
     *
     * @return 是否已初始化
     */
    R<Boolean> isSystemPermissionsInitialized();
}

