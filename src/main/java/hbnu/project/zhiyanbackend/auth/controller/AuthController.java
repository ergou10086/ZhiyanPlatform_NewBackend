package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.service.AuthService;
import hbnu.project.zhiyanbackend.auth.service.PermissionService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证控制器
 * 负责用户注册、登录、密码重置等认证相关功能
 *
 * @author yxy
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/auth")    // 未修改，保持
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "用户注册、登录、验证等认证相关接口")
public class AuthController {

    private final AuthService authService;
    private final PermissionService permissionService;

    /**
     * 发送验证码
     */
    @PostMapping("/send-verfcode")
    @Operation(summary = "发送验证码", description = "向指定邮箱发送验证码，支持注册、重置密码等场景")
    public R<Void> sendVerificationCode(
            @Valid @RequestBody VerificationCodeDTO verificationCodeDTO) {
        log.info("发送验证码请求: 邮箱={}, 类型={}", verificationCodeDTO.getEmail(), verificationCodeDTO.getType());

        return authService.sendVerificationCode(verificationCodeDTO);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过邮箱和验证码进行用户注册")
    public R<UserRegisterResponseDTO> register(
            @Valid @RequestBody RegisterDTO request) {
        log.info("用户注册请求: 邮箱={}, 姓名={}", request.getEmail(), request.getName());

        // 直接调用 auth 模块的服务
        return authService.register(request);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取访问令牌")
    public R<UserLoginResponseDTO> login(
            @Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        log.info("用户登录API请求: 邮箱={}", loginBody.getEmail());


    }
}
