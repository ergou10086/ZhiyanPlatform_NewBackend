package hbnu.project.zhiyanbackend.auth.service;

import hbnu.project.zhiyanbackend.auth.model.dto.AvatarDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserUpdateDTO;
import hbnu.project.zhiyanbackend.basic.domain.R;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 * 提供用户信息管理、权限查询等核心功能
 *
 * @author ErgouTree
 */
public interface UserService {

    /**
     * 获取当前用户的基本信息（不含角色和权限）
     *
     * @param userId 用户ID
     * @return 用户基本信息
     */
    R<UserDTO> getCurrentUser(Long userId);

    /**
     * 获取当前用户的详细信息（包含角色和权限）
     *
     * @param userId 用户ID
     * @return 用户详细信息（包含角色列表和权限列表）
     */
    R<UserDTO> getUserWithRolesAndPermissions(Long userId);

    /**
     * 根据邮箱查询用户信息
     *
     * @param email 用户邮箱
     * @return 用户信息
     */
    R<UserDTO> getUserByEmail(String email);

    /**
     * 根据用户名查询用户信息
     *
     * @param name 用户名
     * @return 用户信息
     */
    R<UserDTO> getUserByName(String name);

    /**
     * 批量根据用户ID查询用户信息
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    R<List<UserDTO>> getUsersByIds(List<Long> userIds);

    /**
     * 分页查询用户列表
     * 支持关键词搜索，开放接口
     *
     * @param pageable 分页参数
     * @param keyword 搜索关键词（可选）
     * @return 用户列表（分页）
     */
    R<Page<UserDTO>> getUserList(Pageable pageable, String keyword);

    /**
     * 锁定用户
     * 管理员功能
     *
     * @param userId 用户ID
     * @param isLocked 是否锁定（true=锁定，false=解锁）
     * @return 操作结果
     */
    R<Void> lockUser(Long userId, boolean isLocked);

    /**
     * 软删除用户
     * 用户自己删除自己的账户（注销）或管理员功能
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    R<Void> deleteUser(Long userId);

    /**
     * 获取用户的所有角色
     *
     * @param userId 用户ID
     * @return 角色名称列表
     */
    R<List<String>> getUserRoles(Long userId);

    /**
     * 获取用户的所有权限
     *
     * @param userId 用户ID
     * @return 权限名称列表
     */
    R<List<String>> getUserPermissions(Long userId);

    /**
     * 检查用户是否拥有指定权限
     * 用于权限校验流程
     *
     * @param userId 用户ID
     * @param permission 权限标识符
     * @return 是否拥有该权限
     */
    R<Boolean> hasPermission(Long userId, String permission);

    /**
     * 批量检查用户是否拥有多个权限
     * 用于一次性校验多个权限
     *
     * @param userId 用户ID
     * @param permissions 权限标识符列表
     * @return 权限校验结果Map（权限 -> 是否拥有）
     */
    R<Map<String, Boolean>> hasPermissions(Long userId, List<String> permissions);

    /**
     * 检查用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param roleName 角色名称
     * @return 是否拥有该角色
     */
    R<Boolean> hasRole(Long userId, String roleName);

    /**
     * 搜索用户
     * 根据邮箱或姓名模糊搜索用户
     *
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 用户列表
     */
    R<Page<UserDTO>> searchUsers(String keyword, Pageable pageable);

    /**
     * 修改用户个人简介
     *
     * @param userId 用户id
     * @param description 个人简介
     * @return 修改状态
     */
    R<Void> updateUserDescription(Long userId, String description);
}

