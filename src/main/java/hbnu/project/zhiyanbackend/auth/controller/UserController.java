package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserInfoResponseDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.UserService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理相关控制器
 * 提供用户信息管理、用户查询、权限查看等功能
 *
 * @author ErgouTree
 * @version 24892734723497285837453789342527836457825342538479352478992547823457897823547847253478923457890234578923457890278354902036748527019708`81479789`318-09278423`167.0
 * @rewrite Tokito
 * @Re_rewrite ErgouTree,asddjv
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/auth/users")     // 原/zhiyan/users
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息管理相关接口")
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;

    private final UserConverter userConverter;

    /**
     * 获取当前登录用户信息
     * 权限: 所有已登录用户
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的基本信息")
    public R<UserInfoResponseDTO> getCurrentUser() {
        log.info("获取当前用户信息");

        try{
            // 从 Security Context 中获取当前用户ID
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录");
            }

            // 调用服务层获取用户信息
            R<UserDTO> result = userService.getCurrentUser(userId);
            if(!R.isSuccess(result)){
                return R.fail(result.getMsg());
            }

            // 转换为Response对象(不包含用户权限)
            UserDTO userDTO = result.getData();
            UserInfoResponseDTO response = userConverter.toUserInfoResponse(userDTO);

            return R.ok(response);
        }catch (ControllerException e){
            log.error("获取当前用户信息失败", e);
            return R.fail("获取用户信息失败");
        }
    }


    /**
     * 根据ID获取用户详细信息（包含角色和权限）
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息（包含角色和权限）")
    public R<UserInfoResponseDTO>  getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("获取用户详情: 用户ID={}", userId);

        try {
            // 调用服务层获取用户详细信息
            R<UserDTO> result = userService.getUserWithRolesAndPermissions(userId);
            if (!R.isSuccess(result)) {
                return R.fail(result.getMsg());
            }

            // 转换为Response对象（包含角色和权限）
            UserDTO userDTO = result.getData();
            UserInfoResponseDTO response = userConverter.toUserInfoResponseDTOwithRoles(userDTO);

            return R.ok(response);
        } catch (Exception e) {
            log.error("获取用户详情失败: userId={}", userId, e);
            return R.fail("获取用户详情失败");
        }
    }


    /**
     * 分页获取用户列表
     * 需登录
     */
    @GetMapping("/user-list")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取用户列表", description = "分页查询用户列表（管理员功能）")
    public R<Page<UserInfoResponseDTO>> getUserList(
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "搜索关键词（邮箱、姓名、机构）")
            @RequestParam(required = false) String keyword) {
        log.info("获取用户列表: 页码={}, 每页数量={}, 关键词={}", page, size, keyword);

        try {
            // 创建分页参数（按创建时间降序）
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // 调用服务层查询用户列表
            R<Page<UserDTO>> result = userService.getUserList(pageable, keyword);
            if (!R.isSuccess(result)) {
                return R.fail(result.getMsg());
            }

            // 转换为Response对象
            Page<UserDTO> userPage = result.getData();
            Page<UserInfoResponseDTO> responsePage = userPage.map(userConverter::toUserInfoResponse);

            return R.ok(responsePage);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return R.fail("获取用户列表失败");
        }
    }


    /**
     * 搜索用户（用于项目成员邀请等场景）
     * 路径: GET /api/users/search
     * 权限: 所有已登录用户
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "根据关键词搜索用户（用于成员邀请等场景）")
    public R<Page<UserInfoResponseDTO>> searchUsers(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam String keyword,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "10") int size) {
        log.info("搜索用户: 关键词={}, 页码={}, 每页数量={}", keyword, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            R<Page<UserDTO>> result = userService.searchUsers(keyword, pageable);

            if (!R.isSuccess(result)) {
                return R.fail(result.getMsg());
            }

            Page<UserDTO> userPage = result.getData();
            Page<UserInfoResponseDTO> responsePage = userPage.map(userConverter::toUserInfoResponse);

            return R.ok(responsePage);
        } catch (Exception e) {
            log.error("搜索用户失败: keyword={}", keyword, e);
            return R.fail("搜索用户失败");
        }
    }


    /**
     * 获取用户研究方向标签
     * 需登录
     */
    @GetMapping("/profile/research-tags")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取研究方向标签", description = "获取当前用户的研究方向标签")
    public R<List<String>> getResearchTags() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或令牌无效");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("用户不存在"));

        return R.ok(user.getResearchTagList());
    }

    /**
     * 锁定用户
     * 角色: DEVELOPER（只有开发者可以锁定用户）
     */
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "锁定用户", description = "锁定指定用户，禁止其登录（管理员功能）")
    public R<Void> lockUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("锁定用户: 用户ID={}", userId);

        try {
            return userService.lockUser(userId, true);
        } catch (Exception e) {
            log.error("锁定用户失败: userId={}", userId, e);
            return R.fail("锁定用户失败");
        }
    }


    /**
     * 解锁用户
     * 角色: DEVELOPER（只有开发者可以解锁用户）
     */
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "解锁用户", description = "解锁指定用户，允许其登录（管理员功能）")
    public R<Void> unlockUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("解锁用户: 用户ID={}", userId);

        try {
            return userService.lockUser(userId, false);
        } catch (Exception e) {
            log.error("解锁用户失败: userId={}", userId, e);
            return R.fail("解锁用户失败");
        }
    }


    /**
     * 软删除用户(销号)
     * 需登录，只有自己和开发者可以销号
     * 角色: DEVELOPER（只有开发者可以删除用户）
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "删除用户", description = "软删除指定用户（管理员功能）")
    public R<Void> deleteUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("删除用户: 用户ID={}", userId);

        try {
            // 可以自己删除自己
            Long currentUserId = SecurityUtils.getUserId();
            if (userId.equals(currentUserId)) {
                return userService.deleteUser(userId);
            }

            return userService.deleteUser(userId);
        } catch (Exception e) {
            log.error("删除用户失败: userId={}", userId, e);
            return R.fail("删除用户失败");
        }
    }


    /**
     * 检查用户是否拥有指定权限
     * 用于权限校验流程
     * 该接口无需权限校验
     */
    @PostMapping("/{userId}/has-permission")
    @Operation(summary = "检查用户权限", description = "检查用户是否拥有指定权限（内部接口）")
    public R<Boolean> hasPermission(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "权限标识符", required = true)
            @RequestParam String permission) {
        log.debug("检查用户权限: userId={}, permission={}", userId, permission);

        try {
            return userService.hasPermission(userId, permission);
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permission={}", userId, permission, e);
            return R.fail("权限检查失败");
        }
    }


    /**
     * 批量检查用户权限（内部接口）
     * 用于一次性校验多个权限
     * 无需权限校验
     */
    @PostMapping("/{userId}/has-permissions")
    @Operation(summary = "批量检查用户权限", description = "批量检查用户是否拥有多个权限（内部接口）")
    public R<Map<String, Boolean>> hasPermissions(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "权限标识符列表", required = true)
            @RequestBody List<String> permissions) {
        log.debug("批量检查用户权限: userId={}, permissions数量={}", userId, permissions.size());

        try {
            return userService.hasPermissions(userId, permissions);
        } catch (Exception e) {
            log.error("批量检查用户权限失败: userId={}", userId, e);
            return R.fail("批量权限检查失败");
        }
    }


    /**
     * 检查用户是否拥有指定角色（内部接口）
     * 用于角色校验
     * 无需权限校验（内部调用）
     */
    @PostMapping("/{userId}/has-role")
    @Operation(summary = "检查用户角色", description = "检查用户是否拥有指定角色（内部接口）")
    public R<Boolean> hasRole(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "角色名称", required = true)
            @RequestParam String roleName) {
        log.debug("检查用户角色: userId={}, roleName={}", userId, roleName);

        try {
            return userService.hasRole(userId, roleName);
        } catch (Exception e) {
            log.error("检查用户角色失败: userId={}, roleName={}", userId, roleName, e);
            return R.fail("角色检查失败");
        }
    }
}
