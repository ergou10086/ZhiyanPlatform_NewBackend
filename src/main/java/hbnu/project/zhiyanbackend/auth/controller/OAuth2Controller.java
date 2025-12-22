package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.oauth.client.OAuth2Client;
import hbnu.project.zhiyanbackend.auth.oauth.config.properties.OAuth2Properties;
import hbnu.project.zhiyanbackend.auth.oauth.provider.OrcidOAuth2Provider;
import hbnu.project.zhiyanbackend.auth.repository.UserConnectionRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.OAuth2Service;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OAuth2第三方登录控制器
 * 处理OAuth2授权和回调，重构后采用"主账号 + 绑定关系"模式
 *
 * @author ErgouTree
 * @modify yui
 * @rewrite ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2第三方登录", description = "GitHub等第三方登录相关接口")
public class OAuth2Controller {

    @Resource
    private OAuth2Client oAuth2Client;
    private final OAuth2Service oAuth2Service;
    private final OAuth2Properties oAuth2Properties;
    private final ObjectMapper objectMapper;
    private final OrcidOAuth2Provider orcidOAuth2Provider;
    private final UserRepository userRepository;
    private final UserConnectionRepository userConnectionRepository;

    /**
     * 获取授权 URL（前端跳转用）
     */
    @GetMapping("/authorize/{provider}")
    @Operation(summary = "获取 OAuth2 授权 URL", description = "获取第三方登录的授权 URL，用户需要跳转到该 URL 完成授权")
    public R<AuthorizationResultDTO> getAuthorizationUrl(
            @Parameter(description = "OAuth2 提供商名称", example = "github", required = true)
            @PathVariable String provider) {
        log.info("获取 OAuth2 授权 URL 请求 - 提供商: {}", provider);

        try {
            String redirectUri = buildCallbackUrl(provider);
            AuthorizationResultDTO result = oAuth2Client.getAuthorizationUrl(provider, redirectUri);
            return R.ok(result, "获取授权 URL 成功");
        } catch (Exception e) {
            log.error("获取 OAuth2 授权 URL 失败 - 提供商: {}, 错误: {}", provider, e.getMessage(), e);
            return R.fail("获取授权 URL 失败: " + e.getMessage());
        }
    }


    /**
     * OAuth2 回调接口（第三方平台授权后跳转）
     *
     * 核心逻辑：
     * 1. 获取 OAuth2 用户信息
     * 2. 调用 OAuth2Service.handleOAuth2Login 处理登录/注册
     * 3. 重定向到前端（携带 token 或错误信息）
     */
    @GetMapping("/callback/{provider}")
    @Operation(summary = "OAuth2 回调", description = "第三方平台授权成功后的回调接口")
    public void callback(
            @Parameter(description = "OAuth2 提供商名称", example = "github", required = true)
            @PathVariable String provider,
            @Parameter(description = "授权码", required = true)
            @RequestParam String code,
            @Parameter(description = "状态参数（用于防 CSRF 攻击）", required = true)
            @RequestParam String state,
            HttpServletResponse response) {
        log.info("OAuth2 回调请求 - 提供商: {}, code: {}, state: {}", provider, code, state);

        try {
            // 1. 构建回调 URL
            String redirectUri = buildCallbackUrl(provider);

            // 2. 通过授权码获取用户信息
            OAuth2UserInfoDTO userInfo = oAuth2Client.getUserInfoByCode(provider, code, state, redirectUri);
            log.info("获取 OAuth2 用户信息成功 - 提供商: {}, 用户ID: {}, 邮箱: {}",
                    provider, userInfo.getProviderUserId(), userInfo.getEmail());

            // 3. 处理登录/注册
            R<OAuth2LoginResponseDTO> loginResult = oAuth2Service.handleOAuth2Login(userInfo);

            // 4. 重定向到前端
            String redirectUrl = buildSuccessRedirectUrl(loginResult);
            log.info("重定向到前端页面: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 回调处理失败 - 提供商: {}, 错误: {}", provider, e.getMessage(), e);
            // 重定向到前端错误页面
            try {
                String errorUrl = buildErrorRedirectUrl(provider, e.getMessage());
                response.sendRedirect(errorUrl);
            } catch (Exception ex) {
                log.error("重定向到错误页面失败", ex);
            }
        }
    }


    /**
     * 已登录用户手动绑定第三方账号
     *
     * 流程：
     * 1. 用户点击"绑定 GitHub"
     * 2. 跳转到 /oauth2/authorize/github
     * 3. 授权完成后回调到 /oauth2/bind/callback/{provider}
     * 4. 调用此接口完成绑定
     */
    @GetMapping("/bind/callback/{provider}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "手动绑定第三方账号回调", description = "已登录用户绑定第三方账号的回调接口")
    public void bindCallback(
            @Parameter(description = "OAuth2 提供商名称", example = "github", required = true)
            @PathVariable String provider,
            @Parameter(description = "授权码", required = true)
            @RequestParam String code,
            @Parameter(description = "状态参数", required = true)
            @RequestParam String state,
            HttpServletResponse response) {
        log.info("手动绑定回调 - 提供商: {}", provider);

        try{
            // 1. 获取当前用户ID
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new IllegalStateException("用户未登录");
            }

            // 2. 获取 OAuth2 用户信息
            String redirectUri = buildBindCallbackUrl(provider);
            OAuth2UserInfoDTO userInfo = oAuth2Client.getUserInfoByCode(provider, code, state, redirectUri);

            // 3.执行绑定
            R<Void> bindResult = oAuth2Service.bindOAuth2Account(userId, userInfo);

            // 4.重定向到前端
            String redirectUrl;
            if(R.isSuccess(bindResult)){
                redirectUrl = buildBindSuccessRedirectUrl(provider);
            } else {
                redirectUrl = buildBindErrorRedirectUrl(provider, bindResult.getMsg());
            }
            response.sendRedirect(redirectUrl);
        }catch (ControllerException | IOException e){
            log.error("手动绑定失败 - 提供商: {}", provider, e);
            try {
                String errorUrl = buildBindErrorRedirectUrl(provider, e.getMessage());
                response.sendRedirect(errorUrl);
            } catch (Exception ex) {
                log.error("重定向失败", ex);
            }
        }
    }


    /**
     * 解绑第三方账号
     */
    @DeleteMapping("/unbind/{provider}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "解绑第三方账号", description = "解除当前用户与指定第三方平台的绑定关系")
    public R<Void> unbindAccount(
            @Parameter(description = "OAuth2 提供商名称", example = "github", required = true)
            @PathVariable String provider) {
        log.info("解绑第三方账号请求 - 提供商: {}", provider);

        try {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录");
            }

            return oAuth2Service.unbindOAuth2Account(userId, provider);
        } catch (Exception e) {
            log.error("解绑第三方账号失败 - 提供商: {}", provider, e);
            return R.fail("解绑失败: " + e.getMessage());
        }
    }


    /**
     * 获取当前用户的绑定列表
     */
    @GetMapping("/connections")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取绑定列表", description = "获取当前用户的所有第三方账号绑定关系")
    public R<List<UserConnectionDTO>> getUserConnections() {
        try{
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录");
            }

            return oAuth2Service.getUserConnections(userId);
        }catch (Exception e){
            log.error("获取绑定列表失败", e);
            return R.fail("获取失败");
        }
    }


    /**
     * 获取ORCID用户详细信息
     * 包括Keywords、Employment和Education
     *
     * @return ORCID详细信息
     */
    @GetMapping("/orcid/detail")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取ORCID详细信息", description = "获取当前用户的ORCID Keywords、工作经历和教育经历")
    public R<OrcidDetailDTO> getOrcidDetail() {
        try {
            // 1. 获取当前用户ID
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录");
            }

            // 2. 调用服务层方法获取详细信息
            return oAuth2Service.getOrcidDetail(userId);

        } catch (Exception e) {
            log.error("获取ORCID详细信息异常", e);
            return R.fail("获取失败，请稍后重试");
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建回调 URL（用于 OAuth2 登录）
     */
    private String buildCallbackUrl(String provider) {
        String baseUrl = oAuth2Properties.getCallbackBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("OAuth2 回调地址基础路径未配置");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/zhiyan/auth/oauth2/callback/" + provider;
    }

    /**
     * 构建绑定回调 URL（用于手动绑定）
     */
    private String buildBindCallbackUrl(String provider) {
        String baseUrl = oAuth2Properties.getCallbackBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("OAuth2 回调地址基础路径未配置");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/zhiyan/auth/oauth2/bind/callback/" + provider;
    }

    /**
     * 构建登录成功重定向 URL
     */
    private String buildSuccessRedirectUrl(R<OAuth2LoginResponseDTO> loginResult) {
        String homeUrl = oAuth2Properties.getFrontendHomeUrl();
        if (StringUtils.isBlank(homeUrl)) {
            homeUrl = "http://localhost:8080";
        }
        if (homeUrl.endsWith("/")) {
            homeUrl = homeUrl.substring(0, homeUrl.length() - 1);
        }

        if (!R.isSuccess(loginResult) || loginResult.getData() == null) {
            return homeUrl + "?oauth2=error&message=" +
                    URLEncoder.encode(loginResult.getMsg(), StandardCharsets.UTF_8);
        }

        OAuth2LoginResponseDTO response = loginResult.getData();
        StringBuilder url = new StringBuilder(homeUrl);
        url.append("?oauth2=success");

        if (response.getLoginResponse() != null) {
            url.append("&token=").append(URLEncoder.encode(
                    response.getLoginResponse().getAccessToken(), StandardCharsets.UTF_8));
            if (response.getLoginResponse().getRefreshToken() != null) {
                url.append("&refreshToken=").append(URLEncoder.encode(
                        response.getLoginResponse().getRefreshToken(), StandardCharsets.UTF_8));
            }
        }

        return url.toString();
    }

    /**
     * 构建错误重定向 URL
     */
    private String buildErrorRedirectUrl(String provider, String errorMessage) {
        String errorUrl = oAuth2Properties.getFrontendErrorUrl();
        if (StringUtils.isBlank(errorUrl)) {
            errorUrl = "http://localhost:8080/login";
        }
        if (errorUrl.endsWith("/")) {
            errorUrl = errorUrl.substring(0, errorUrl.length() - 1);
        }
        return errorUrl + "?oauth2=error&provider=" + provider +
                "&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
    }

    /**
     * 构建绑定成功重定向 URL
     */
    private String buildBindSuccessRedirectUrl(String provider) {
        String settingsUrl = oAuth2Properties.getFrontendHomeUrl();
        if (StringUtils.isBlank(settingsUrl)) {
            settingsUrl = "http://localhost:8080/settings";
        }
        return settingsUrl + "?bind=success&provider=" + provider;
    }

    /**
     * 构建绑定失败重定向 URL
     */
    private String buildBindErrorRedirectUrl(String provider, String errorMessage) {
        String settingsUrl = oAuth2Properties.getFrontendHomeUrl();
        if (StringUtils.isBlank(settingsUrl)) {
            settingsUrl = "http://localhost:8080/settings";
        }
        return settingsUrl + "?bind=error&provider=" + provider +
                "&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
    }
}