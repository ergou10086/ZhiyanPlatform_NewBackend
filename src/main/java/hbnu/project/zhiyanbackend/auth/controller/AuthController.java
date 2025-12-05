package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.exeption.CookieException;
import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.service.AuthService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;

import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
     * 验证验证码
     */
    @PostMapping("/verify-code")
    @Operation(summary = "验证验证码", description = "单独验证验证码是否正确")
    public R<Boolean> verifyCode(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String type) {
        log.info("验证验证码API请求: 邮箱={}, 类型={}", email, type);

        return authService.verifyCode(email, code, type);
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
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("用户登录API请求: 邮箱={}", loginDTO.getEmail());

        try{
            R<UserLoginResponseDTO> result = authService.login(loginDTO, request);

            // 添加RemembermeToken
            if(R.isSuccess(result) && result.getData() != null && Boolean.TRUE.equals(result.getData().getRememberMe()) && result.getData().getRememberMeToken() != null){
                try {
                    Cookie cookie = new Cookie("remember_me_token", result.getData().getRememberMeToken());
                    cookie.setHttpOnly(true);
                    cookie.setPath("/");
                    cookie.setMaxAge(30 * 24 * 60 * 60);
                    response.addCookie(cookie);
                    log.debug("已添加RememberMe Cookie");
                } catch (CookieException e) {
                    log.warn("添加RememberMe Cookie失败", e);
                }
            }

            return result;
        }catch (ControllerException e){
            log.error("登录处理异常", e);
            return R.fail("登录失败：" + e.getMessage());
        }
    }

    /**
     * 自动登录检查接口
     * 前端可以在应用启动时调用此接口检查是否可以通过RememberMe自动登录
     */
    @GetMapping("/auto-login-check")
    @Operation(summary = "自动登录检查", description = "检查是否存在有效的RememberMe token")
    public R<AutoLoginCheckResponseDTO> autoLoginCheck(
            @CookieValue(value = "remember_me_token", required = false) String rememberMeToken) {
        log.info("自动登录检查请求: token存在={}", StringUtils.isNotBlank(rememberMeToken));

        if (StringUtils.isBlank(rememberMeToken)) {
            return R.ok(AutoLoginCheckResponseDTO.noToken(), "无有效的RememberMe token");
        }

        Optional<Long> userIdOpt = authService.validateRememberMeToken(rememberMeToken);
        if (userIdOpt.isPresent()) {
            // token有效，可以自动登录
            Long userId = userIdOpt.get();
            log.info("自动登录检查成功: 用户ID={}", userId);
            return R.ok(AutoLoginCheckResponseDTO.valid(userId), "存在有效的RememberMe token");
        }else{
            // token无效
            log.info("自动登录检查失败: token已过期或无效");
            return R.ok(AutoLoginCheckResponseDTO.invalid(), "RememberMe token已过期");
        }
    }

    /**
     * 测试接口 - 验证JWT Token是否被正确携带
     * 用于前端诊断Token传递问题
     */
    @GetMapping("/debug/token-check")
    @Operation(summary = "Token诊断接口", description = "检查请求是否正确携带JWT Token")
    public R<Map<String, Object>> debugTokenCheck(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("timestamp", System.currentTimeMillis());
        debugInfo.put("requestPath", request.getRequestURI());

        // 检查 Authorization header
        debugInfo.put("hasAuthHeader", authHeader != null);
        if (authHeader != null) {
            debugInfo.put("authHeaderLength", authHeader.length());
            debugInfo.put("startsWithBearer", authHeader.startsWith("Bearer "));

            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                debugInfo.put("tokenLength", token.length());

                // 尝试验证token
                try {
                    String userId = authService.validateToken(token);
                    debugInfo.put("tokenValid", userId != null);
                    debugInfo.put("userId", userId);

                    if (userId != null) {
                        // 获取token详细信息
                        TokenValidateResponseDTO validateResponse = authService.validateTokenWithDetails(token).getData();
                        debugInfo.put("roles", validateResponse.getRoles());
                        debugInfo.put("remainingTime", validateResponse.getRemainingTime());
                    }
                } catch (Exception e) {
                    debugInfo.put("tokenValid", false);
                    debugInfo.put("error", e.getMessage());
                }
            }
        } else {
            debugInfo.put("message", "缺少 Authorization header！前端需要在请求头中添加: Authorization: Bearer <token>");
        }

        log.info("Token诊断 - {}", debugInfo);
        return R.ok(debugInfo, "Token诊断完成");
    }

    /**
     * 清除RememberMe token
     */
    @PostMapping("/clear-remember-me")
    @Operation(summary = "清除记住我", description = "清除RememberMe token")
    public R<Void> clearRememberMe(
            @CookieValue(value = "remember_me_token", required = false) String rememberMeToken,
            HttpServletResponse response) {
        log.info("清除RememberMe token请求");

        if (StringUtils.isNotBlank(rememberMeToken)) {
            // 从数据库中删除token
            authService.deleteRememberMeToken(rememberMeToken);
            log.info("从数据库中删除RememberMe token");
        }

        // 清除Cookie
        Cookie cookie = new Cookie("remember_me_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        log.info("清除RememberMe token成功");
        return R.ok(null, "清除成功");
    }

    /**
     * 检查邮箱是否已注册
     */
    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱", description = "检查邮箱是否已被注册")
    public R<Boolean> checkEmail(@RequestParam String email) {
        log.info("检查邮箱API请求: 邮箱={}", email);

        return authService.checkEmail(email);
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用Refresh Token获取新的Access Token")
    public R<TokenDTO> refreshToken(@Valid @RequestBody TokenRefreshDTO request) {
        log.info("令牌刷新请求 - refreshToken: {}", request.getRefreshToken());
        TokenDTO tokenDTO = authService.refreshToken(request.getRefreshToken());
        return R.ok(tokenDTO);
    }

    /**
     * 验证令牌有效性
     */
    @GetMapping("/validate")
    @Operation(summary = "验证令牌", description = "验证访问令牌是否有效")
    public R<TokenValidateResponseDTO> validateToken(
            @RequestHeader("Authorization") String token) {
        log.info("令牌验证请求");

        // 直接调用服务层完成验证逻辑
        return authService.validateTokenWithDetails(token);
    }

    /**
     * 忘记密码 - 发送重置验证码
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码", description = "发送密码重置验证码到邮箱")
    public R<Void> forgotPassword(@Valid @RequestBody ForgetPasswordDTO request) {
        log.info("忘记密码请求: 邮箱={}", request.getEmail());

        // 调用 service
        return authService.forgotPassword(request);
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "通过验证码重置密码")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        log.info("重置密码请求: 邮箱={}", request.getEmail());

        return authService.resetPassword(request);
    }

    /**
     * 用户登出接口
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出，使令牌失效")
    public R<String> logout(@RequestHeader("Authorization") String tokenHeader) {
        authService.logout(tokenHeader);
        // 返回 R<Void>
        return R.ok(null, "登出成功");
    }

    /**
     * 修改用户邮箱
     */
    @PostMapping("/change-email")
    @Operation(summary = "修改邮箱", description = "通过验证码修改邮箱")
    public R<UserDTO> changeEmail(
            @Valid @RequestBody ChangeEmailDTO request,
            HttpServletResponse response) {
        log.info("修改邮箱请求: 旧邮箱={}，新邮箱={}", request.getOldEmail(), request.getNewEmail());

        try {
            R<UserDTO> result = authService.changeEmail(request);

            // 如果修改成功，清除RememberMe Cookie（强制重新登录）
            if (R.isSuccess(result)) {
                Cookie cookie = new Cookie("remember_me_token", null);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
                log.info("邮箱修改成功，已清除RememberMe Cookie");
            }

            return result;
        } catch (Exception e) {
            log.error("修改邮箱失败", e);
            return R.fail("修改邮箱失败：" + e.getMessage());
        }
    }

    /**
     * 启用2FA - 生成密钥和二维码
     */
    @PostMapping("/2fa/enable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "启用2FA", description = "生成2FA密钥和二维码，需要用户扫描后确认启用")
    public R<TwoFactorSetupDTO> enableTwoFactorAuth() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]请求启用2FA", userId);
        return authService.enableTwoFactorAuth(userId);
    }

    /**
     * 确认启用2FA
     */
    @PostMapping("/2fa/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "确认启用2FA", description = "验证2FA验证码后正式启用")
    public R<Void> confirmEnableTwoFactorAuth(
            @Valid @RequestBody TwoFactorVerifyDTO request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]确认启用2FA", userId);
        return authService.confirmEnableTwoFactorAuth(userId, request.getCode());
    }

    /**
     * 禁用2FA
     */
    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "禁用2FA", description = "需要验证当前2FA验证码才能禁用")
    public R<Void> disableTwoFactorAuth(
            @Valid @RequestBody TwoFactorVerifyDTO request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]请求禁用2FA", userId);
        return authService.disableTwoFactorAuth(userId, request.getCode());
    }
}
