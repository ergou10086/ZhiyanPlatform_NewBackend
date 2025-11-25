package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserInfoResponseDTO;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.UserService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

            // 转换为Response对象
            UserDTO userDTO = result.getData();
            UserInfoResponseDTO response = userConverter.toUserInfoResponseDTO(userDTO);

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
            UserInfoResponseDTO response = userConverter.toUserInfoResponse(userDTO);

            return R.ok(response);
        } catch (Exception e) {
            log.error("获取用户详情失败: userId={}", userId, e);
            return R.fail("获取用户详情失败");
        }
    }

}
