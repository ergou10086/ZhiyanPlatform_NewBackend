package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.Permission;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.repository.PermissionRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.UserService;
import hbnu.project.zhiyanbackend.basic.domain.R;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 提供用户管理、权限查询等核心功能
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PermissionRepository permissionRepository;

    private final UserConverter userConverter;

    /**
     * 获取当前用户的基本信息（不含角色和权限）
     * 用于用户基本信息展示，不涉及权限相关逻辑
     *
     * @param userId 用户ID
     * @return 用户基本信息
     */
    @Override
    @Transactional(readOnly = true)
    public R<UserDTO> getCurrentUser(Long userId) {
        try {
            log.debug("获取当前用户信息 - userId: {}", userId);

            Optional<User> optionalUser = userRepository.findByIdAndIsDeletedFalse(userId);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在或已被删除 - userId: {}", userId);
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();
            UserDTO userDTO = userConverter.toDTO(user);

            log.debug("成功获取用户信息 - userId: {}, email: {}", userId, user.getEmail());
            return R.ok(userDTO);

        } catch (Exception e) {
            log.error("获取用户信息异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return R.fail("获取用户信息失败");
        }
    }

    /**
     * 获取用户详细信息（包含角色和权限）
     * 用于需要权限和角色的常见
     *
     * @param userId 用户ID
     * @return UserDTO用户信息
     */
    @Override
    @Transactional(readOnly = true)
    public R<UserDTO> getUserWithRolesAndPermissions(Long userId) {
        try {
            log.debug("获取用户详细信息（含角色权限） - userId: {}", userId);

            // 查询用户及其角色信息（避免 MultipleBagFetchException）
            Optional<User> optionalUser = userRepository.findByIdWithRolesAndPermissions(userId);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在或已被删除 - userId: {}", userId);
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();

            // 转换为DTO,包含角色和权限
            UserDTO userDTO = userConverter.toDTO(user);

            // 获取角色列表
            List<String> roles = user.getUserRoles().stream()
                    .map(ur -> ur.getRole().getName())
                    .distinct()
                    .collect(Collectors.toList());
            userDTO.setRoles(roles);

            // 单独查询权限（避免N+1问题和懒加载问题）
            List<Permission> permissions = permissionRepository.findAllByUserId(userId);
            List<String> permissionNames = permissions.stream()
                    .map(Permission::getName)
                    .distinct()
                    .collect(Collectors.toList());
            userDTO.setPermissions(permissionNames);

            log.debug("成功获取用户详细信息 - userId: {}, 角色数: {}, 权限数: {}",
                    userId, roles.size(), permissionNames.size());
            return R.ok(userDTO);

        } catch (Exception e) {
            log.error("获取用户详细信息异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return R.fail("获取用户信息失败");
        }
    }

    /**
     * 根据邮箱查询用户信息
     *
     * @param email 用户邮箱
     * @return UserDTO用户信息
     */
    @Override
    @Transactional(readOnly = true)
    public R<UserDTO> getUserByEmail(String email) {
        try {
            log.debug("根据邮箱查询用户 - email: {}", email);

            if (StringUtils.isBlank(email)) {
                return R.fail("邮箱不能为空");
            }

            Optional<User> optionalUser = userRepository.findByEmailAndIsDeletedFalse(email);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在 - email: {}", email);
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();
            UserDTO userDTO = userConverter.toDTO(user);

            log.debug("成功查询到用户 - email: {}, userId: {}", email, user.getId());
            return R.ok(userDTO);

        } catch (Exception e) {
            log.error("根据邮箱查询用户异常 - email: {}, 错误: {}", email, e.getMessage(), e);
            return R.fail("查询用户失败");
        }
    }

    /**
     * 根据用户名查询用户信息
     *
     * @param name 用户名
     * @return UserDTO用户信息
     */
    @Override
    @Transactional(readOnly = true)
    public R<UserDTO> getUserByName(String name) {
        try {
            log.debug("根据姓名查询用户 - name: {}", name);

            if (StringUtils.isBlank(name)) {
                return R.fail("姓名不能为空");
            }

            Optional<User> optionalUser = userRepository.findByNameAndIsDeletedFalse(name);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在 - name: {}", name);
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();
            UserDTO userDTO = userConverter.toDTO(user);

            log.debug("成功查询到用户 - name: {}, userId: {}", name, user.getId());
            return R.ok(userDTO);

        } catch (Exception e) {
            log.error("根据姓名查询用户异常 - name: {}, 错误: {}", name, e.getMessage(), e);
            return R.fail("查询用户失败");
        }
    }

    /**
     * 根据用户id批量查询用户信息
     *
     * @param userIds 用户ID列表
     * @return UserDTO用户信息列表
     */
    @Override
    @Transactional(readOnly = true)
    public R<List<UserDTO>> getUsersByIds(List<Long> userIds) {
        try {
            log.debug("批量查询用户信息 - 数量: {}", userIds.size());

            if (userIds == null || userIds.isEmpty()) {
                return R.ok(Collections.emptyList());
            }

            List<User> users = userRepository.findAllById(userIds);

            // 过滤掉已删除的用户
            List<User> activeUsers = users.stream()
                    .filter(user -> !user.getIsDeleted())
                    .collect(Collectors.toList());

            List<UserDTO> userDTOs = userConverter.toDTOList(activeUsers);

            log.debug("成功批量查询用户 - 请求数: {}, 查询到: {}", userIds.size(), userDTOs.size());
            return R.ok(userDTOs);

        } catch (Exception e) {
            log.error("批量查询用户异常 - 错误: {}", e.getMessage(), e);
            return R.fail("批量查询用户失败");
        }
    }

    /**
     * 分页查询用户列表
     * 项目成员邀请使用
     *
     * @param pageable 分页参数
     * @param keyword 搜索关键词（可选）
     * @return UserDTO分页的用户信息
     */
    @Override
    @Transactional(readOnly = true)
    public R<Page<UserDTO>> getUserList(Pageable pageable, String keyword) {
        try {
            log.debug("查询用户列表 - 页码: {}, 每页数量: {}, 关键词: {}",
                    pageable.getPageNumber(), pageable.getPageSize(), keyword);

            Page<User> userPage;

            if (StringUtils.isNotBlank(keyword)) {
                userPage = userRepository.findByNameContainingOrEmailContainingAndIsDeletedFalse(
                        keyword, keyword, pageable);
            } else {
                userPage = userRepository.findByIsDeletedFalse(pageable);
            }

            List<UserDTO> userDTOs = userConverter.toDTOList(userPage.getContent());
            Page<UserDTO> userDTOPage = new PageImpl<>(userDTOs, pageable, userPage.getTotalElements());

            log.debug("查询用户列表成功 - 总数: {}, 当前页数量: {}",
                    userPage.getTotalElements(), userDTOs.size());
            return R.ok(userDTOPage);

        } catch (Exception e) {
            log.error("查询用户列表异常 - 错误: {}", e.getMessage(), e);
            return R.fail("查询用户列表失败");
        }
    }

    /**
     * 锁定/解锁用户
     *
     * @param userId 用户ID
     * @param isLocked 是否锁定（true=锁定，false=解锁）
     * @return
     */
    @Override
    @Transactional
    public R<Void> lockUser(Long userId, boolean isLocked) {
        try {
            log.info("{}用户 - userId: {}", isLocked ? "锁定" : "解锁", userId);

            Optional<User> optionalUser = userRepository.findByIdAndIsDeletedFalse(userId);
            if (optionalUser.isEmpty()) {
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();
            user.setIsLocked(isLocked);
            userRepository.save(user);

            String action = isLocked ? "锁定" : "解锁";
            log.info("用户{}成功 - userId: {}", action, userId);
            return R.ok(null, "用户" + action + "成功");

        } catch (Exception e) {
            log.error("用户锁定/解锁异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return R.fail("操作失败，请稍后重试");
        }
    }

    /**
     * 软删除用户，管理员和用户自己可以使用
     * 销号
     *
     * @param userId 用户ID
     * @return
     */
    @Override
    @Transactional
    public R<Void> deleteUser(Long userId) {
        try {
            log.info("删除用户 - userId: {}", userId);

            Optional<User> optionalUser = userRepository.findByIdAndIsDeletedFalse(userId);
            if (optionalUser.isEmpty()) {
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();
            user.setIsDeleted(true);
            userRepository.save(user);

            log.info("用户删除成功 - userId: {}", userId);
            return R.ok(null, "用户删除成功");

        } catch (Exception e) {
            log.error("用户删除异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return R.fail("用户删除失败，请稍后重试");
        }
    }

    /**
     * 获取用户的所有角色
     *
     * @param userId 用户ID
     * @return 角色字符串
     */
    @Override
    @Transactional(readOnly = true)
    public R<List<String>> getUserRoles(Long userId) {
        try {
            log.debug("获取用户角色 - userId: {}", userId);

            Optional<User> optionalUser = userRepository.findByIdWithRolesAndPermissions(userId);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在 - userId: {}", userId);
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();
            List<String> roles = user.getUserRoles().stream()
                    .map(ur -> ur.getRole().getName())
                    .distinct()
                    .collect(Collectors.toList());

            log.debug("成功获取用户角色 - userId: {}, 角色数: {}", userId, roles.size());
            return R.ok(roles);

        } catch (Exception e) {
            log.error("获取用户角色异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return R.fail("获取用户角色失败");
        }
    }

    /**
     * 获取用户的所有权限
     *
     * @param userId 用户ID
     * @return String权限字符串
     */
    @Override
    @Transactional(readOnly = true)
    public R<List<String>> getUserPermissions(Long userId) {
        try {
            log.debug("获取用户权限 - userId: {}", userId);

            List<Permission> permissions = permissionRepository.findAllByUserId(userId);
            List<String> permissionNames = permissions.stream()
                    .map(Permission::getName)
                    .distinct()
                    .collect(Collectors.toList());

            log.debug("成功获取用户权限 - userId: {}, 权限数: {}", userId, permissionNames.size());
            return R.ok(permissionNames);

        } catch (Exception e) {
            log.error("获取用户权限异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return R.fail("获取用户权限失败");
        }
    }

    /**
     * 检查用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permission 权限标识符
     * @return 检查结果
     */
    @Override
    @Transactional(readOnly = true)
    public R<Boolean> hasPermission(Long userId, String permission) {
        try {
            log.debug("检查用户权限 - userId: {}, permission: {}", userId, permission);

            if (StringUtils.isBlank(permission)) {
                return R.fail("权限标识符不能为空");
            }

            List<Permission> permissions = permissionRepository.findAllByUserId(userId);
            boolean hasPermission = permissions.stream()
                    .anyMatch(p -> p.getName().equals(permission));

            log.debug("权限检查结果 - userId: {}, permission: {}, result: {}",
                    userId, permission, hasPermission);
            return R.ok(hasPermission);

        } catch (Exception e) {
            log.error("检查用户权限异常 - userId: {}, permission: {}, 错误: {}",
                    userId, permission, e.getMessage(), e);
            return R.fail("权限检查失败");
        }
    }

    /**
     * 检查用户是否拥有多个权限
     *
     * @param userId 用户ID
     * @param permissions 权限标识符列表
     * @return 检查结果
     */
    @Override
    @Transactional(readOnly = true)
    public R<Map<String, Boolean>> hasPermissions(Long userId, List<String> permissions) {
        try {
            log.debug("批量检查用户权限 - userId: {}, permissions数量: {}", userId, permissions.size());

            if (permissions == null || permissions.isEmpty()) {
                return R.ok(Collections.emptyMap());
            }

            List<Permission> userPermissions = permissionRepository.findAllByUserId(userId);
            Set<String> userPermissionSet = userPermissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());

            Map<String, Boolean> resultMap = permissions.stream()
                    .distinct()
                    .collect(Collectors.toMap(
                            p -> p,
                            userPermissionSet::contains
                    ));

            log.debug("批量权限检查完成 - userId: {}, 检查数量: {}", userId, resultMap.size());
            return R.ok(resultMap);

        } catch (Exception e) {
            log.error("批量检查用户权限异常 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return R.fail("批量权限检查失败");
        }
    }

    /**
     * 检查用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param roleName 角色名称
     * @return 检查结果
     */
    @Override
    @Transactional(readOnly = true)
    public R<Boolean> hasRole(Long userId, String roleName) {
        try {
            log.debug("检查用户角色 - userId: {}, roleName: {}", userId, roleName);

            if (StringUtils.isBlank(roleName)) {
                return R.fail("角色名称不能为空");
            }

            Optional<User> optionalUser = userRepository.findByIdWithRolesAndPermissions(userId);
            if (optionalUser.isEmpty()) {
                return R.ok(false);
            }

            User user = optionalUser.get();
            boolean hasRole = user.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole().getName().equals(roleName));

            log.debug("角色检查结果 - userId: {}, roleName: {}, result: {}",
                    userId, roleName, hasRole);
            return R.ok(hasRole);

        } catch (Exception e) {
            log.error("检查用户角色异常 - userId: {}, roleName: {}, 错误: {}",
                    userId, roleName, e.getMessage(), e);
            return R.fail("角色检查失败");
        }
    }

    /**
     * 搜索用户
     *
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return UserDTO分页的用户信息
     */
    @Override
    @Transactional(readOnly = true)
    public R<Page<UserDTO>> searchUsers(String keyword, Pageable pageable) {
        try {
            log.debug("搜索用户 - 关键词: {}, 页码: {}, 每页数量: {}",
                    keyword, pageable.getPageNumber(), pageable.getPageSize());

            if (StringUtils.isBlank(keyword)) {
                return R.fail("搜索关键词不能为空");
            }

            Page<User> userPage;

            // 先将关键词解析为用户ID（纯数字）
            try {
                Long userId = Long.parseLong(keyword.trim());
                Optional<User> userById = userRepository.findByIdAndIsDeletedFalse(userId);
                if (userById.isPresent()) {
                    List<User> users = List.of(userById.get());
                    userPage = new PageImpl<>(users, pageable, 1);
                    log.debug("按用户ID搜索成功 - ID: {}", userId);
                // 然后按照用户名搜
                } else {
                    userPage = userRepository.findByNameContainingOrEmailContainingAndIsDeletedFalse(
                            keyword, keyword, pageable);
                }
            // 如果不是纯数字，肯定就是按照邮箱搜了
            } catch (NumberFormatException e) {
                userPage = userRepository.findByNameContainingOrEmailContainingAndIsDeletedFalse(
                        keyword, keyword, pageable);
            }

            List<UserDTO> userDTOs = userConverter.toDTOList(userPage.getContent());
            Page<UserDTO> userDTOPage = new PageImpl<>(userDTOs, pageable, userPage.getTotalElements());

            log.debug("搜索用户成功 - 关键词: {}, 找到: {}个", keyword, userPage.getTotalElements());
            return R.ok(userDTOPage);

        } catch (Exception e) {
            log.error("搜索用户异常 - 关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return R.fail("搜索用户失败");
        }
    }
}
