package hbnu.project.zhiyanbackend.auth.repository;

import hbnu.project.zhiyanbackend.auth.model.entity.Role;
import hbnu.project.zhiyanbackend.auth.model.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色权限关联数据访问接口
 *
 * @author ErgouTree
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * 根据角色ID和权限ID查找角色权限关联
     *
     * @param roleId       角色ID
     * @param permissionId 权限ID
     * @return 角色权限关联对象（可能为空）
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    Optional<RolePermission> findByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 根据角色ID查找角色权限关联列表
     *
     * @param roleId 角色ID
     * @return 角色权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId")
    List<RolePermission> findByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID查找角色权限关联列表
     *
     * @param permissionId 权限ID
     * @return 角色权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.permission.id = :permissionId")
    List<RolePermission> findByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 删除角色的所有权限关联
     *
     * @param roleId 角色ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除指定角色的指定权限关联
     *
     * @param roleId       角色ID
     * @param permissionId 权限ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    int deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 根据角色查找角色权限关联列表
     *
     * @param role 角色对象
     * @return 角色权限关联列表
     */
    List<RolePermission> findByRole(Role role);

    /**
     * 根据角色和权限名称列表查找角色权限关联
     *
     * @param role            角色对象
     * @param permissionNames 权限名称列表
     * @return 角色权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND rp.permission.name IN :permissionNames")
    List<RolePermission> findByRoleAndPermissionNameIn(@Param("role") Role role, @Param("permissionNames") List<String> permissionNames);
}

