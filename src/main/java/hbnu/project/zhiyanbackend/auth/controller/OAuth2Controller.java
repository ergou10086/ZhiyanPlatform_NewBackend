package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.exeption.OAuth2Exception;
import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.oauth.client.OAuth2Client;
import hbnu.project.zhiyanbackend.auth.oauth.config.properties.OAuth2Properties;
import hbnu.project.zhiyanbackend.auth.oauth.provider.OrcidOAuth2Provider;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.OAuth2Service;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * OAuth2第三方登录控制器
 * 处理OAuth2授权和回调
 *
 * @author ErgouTree
 * @rewrite yui
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

    /**
     * 获取授权URL
     * 前端调用此接口获取第三方登录的授权URL，然后跳转到该URL进行授权
     *
     * @param provider 提供商名称（如：github）
     * @return 授权URL和state
     */
    @GetMapping("/authorize/{provider}")
    @Operation(summary = "获取OAuth2授权URL", description = "获取第三方登录的授权URL，用户需要跳转到该URL完成授权")
    public R<AuthorizationResultDTO> getAuthorizationUrl(
            @Parameter(description = "OAuth2提供商名称", example = "github", required = true)
            @PathVariable String provider) {
        log.info("获取OAuth2授权URL请求 - 提供商: {}", provider);

        try {
            String redirectUri = buildCallbackUrl(provider);
            AuthorizationResultDTO result = oAuth2Client.getAuthorizationUrl(provider, redirectUri);
            return R.ok(result, "获取授权URL成功");
        } catch (Exception e) {
            log.error("获取OAuth2授权URL失败 - 提供商: {}, 错误: {}", provider, e.getMessage(), e);
            return R.fail("获取授权URL失败: " + e.getMessage());
        }
    }

    /**
     * OAuth2回调接口
     * 第三方平台授权成功后，会回调到此接口，携带授权码code和state
     * 后端根据登录状态重定向到不同的前端页面：
     * - SUCCESS: 跳转到主页，通过URL参数传递token
     * - NEED_SUPPLEMENT: 跳转到补充信息页面，通过URL参数传递OAuth2用户信息
     * - NEED_BIND: 跳转到绑定页面，通过URL参数传递OAuth2用户信息
     *
     * @param provider 提供商名称
     * @param code     授权码
     * @param state    状态参数（用于防CSRF攻击）
     * @return 重定向到前端对应页面
     */
    @GetMapping("/callback/{provider}")
    @Operation(summary = "OAuth2回调", description = "第三方平台授权成功后的回调接口，根据登录状态重定向到不同前端页面")
    public void callback(
            @Parameter(description = "OAuth2提供商名称", example = "github", required = true)
            @PathVariable String provider,
            @Parameter(description = "授权码", required = true)
            @RequestParam String code,
            @Parameter(description = "状态参数（用于防CSRF攻击）", required = true)
            @RequestParam String state,
            HttpServletResponse response) {
        log.info("OAuth2回调请求 - 提供商: {}, code: {}, state: {}", provider, code, state);

        try {
            // 1. 构建回调URL
            String redirectUri = buildCallbackUrl(provider);

            // 2. 通过授权码获取用户信息
            OAuth2UserInfoDTO userInfo = oAuth2Client.getUserInfoByCode(provider, code, state, redirectUri);
            log.info("获取OAuth2用户信息成功 - 提供商: {}, 用户ID: {}, 邮箱: {}",
                    provider, userInfo.getProviderUserId(), userInfo.getEmail());

            // 3. 处理登录（可能返回登录成功、需要绑定、需要补充信息等状态）
            R<OAuth2LoginResponseDTO> loginResult = oAuth2Service.handleOAuth2Login(userInfo);

            // 4. 根据登录状态重定向到不同的前端页面
            String redirectUrl = buildRedirectUrlByStatus(provider, loginResult, code, state);
            log.info("重定向到前端页面: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2回调处理失败 - 提供商: {}, 错误: {}", provider, e.getMessage(), e);
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
     * 绑定已有账号
     * 当OAuth2邮箱匹配到已有账号时，用户输入密码验证后绑定
     *
     * @param bindBody 绑定请求体
     * @return 登录结果
     */
    @PostMapping("/bind")
    @Operation(summary = "绑定已有账号", description = "将OAuth2账号绑定到已有账号（需要验证密码）")
    public R<OAuth2LoginResponseDTO> bindAccount(
            @Valid @RequestBody OAuth2BindAccountDTO bindBody) {
        log.info("绑定OAuth2账号请求 - 提供商: {}, 邮箱: {}",
                bindBody.getProvider(), bindBody.getEmail());

        try {
            R<OAuth2LoginResponseDTO> result = oAuth2Service.bindAccount(bindBody);

            // 如果绑定成功，返回登录响应，前端可以根据状态跳转到主页
            if (R.isSuccess(result) && result.getData() != null
                    && result.getData().getStatus() == OAuth2LoginResponseDTO.OAuth2LoginStatus.SUCCESS) {
                log.info("OAuth2绑定成功，返回登录响应");
            }

            return result;
        } catch (Exception e) {
            log.error("绑定OAuth2账号失败 - 提供商: {}, 错误: {}",
                    bindBody.getProvider(), e.getMessage(), e);
            return R.fail("绑定失败: " + e.getMessage());
        }
    }

    /**
     * 补充信息创建账号
     * 当OAuth2信息不足（如缺少邮箱）时，用户补充必要信息（邮箱、密码）后创建账号
     *
     * @param supplementBody 补充信息请求体
     * @return 登录结果
     */
    @PostMapping("/supplement")
    @Operation(summary = "补充信息创建账号", description = "补充必要信息（邮箱、密码）后创建新账号")
    public R<OAuth2LoginResponseDTO> supplementInfo(
            @Valid @RequestBody OAuth2SupplementInfoDTO supplementBody) {
        log.info("补充信息创建账号请求 - 提供商: {}, 邮箱: {}",
                supplementBody.getProvider(), supplementBody.getEmail());

        try {
            R<OAuth2LoginResponseDTO> result = oAuth2Service.supplementInfoAndCreateAccount(supplementBody);

            // 如果创建成功，返回登录响应，前端可以根据状态跳转到主页
            if (R.isSuccess(result) && result.getData() != null
                    && result.getData().getStatus() == OAuth2LoginResponseDTO.OAuth2LoginStatus.SUCCESS) {
                log.info("OAuth2账号创建成功，返回登录响应");
            }

            return result;
        } catch (Exception e) {
            log.error("补充信息创建账号失败 - 提供商: {}, 错误: {}",
                    supplementBody.getProvider(), e.getMessage(), e);
            return R.fail("创建账号失败: " + e.getMessage());
        }
    }

    /**
     * 构建回调URL
     * 根据配置的回调基础路径和提供商名称构建完整的回调URL
     */
    private String buildCallbackUrl(String provider) {
        String baseUrl = oAuth2Properties.getCallbackBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("OAuth2回调地址基础路径未配置，请在配置文件中设置 zhiyan.oauth2.callback-base-url");
        }

        // 移除末尾的斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // 构建完整的回调URL
        // 例如：http://localhost:8090/zhiyan/auth/oauth2/callback/github
        return baseUrl + "/zhiyan/auth/oauth2/callback/" + provider;
    }

    /**
     * 根据登录状态构建重定向URL
     * 根据不同状态跳转到不同的前端页面
     */
    private String buildRedirectUrlByStatus(String provider, R<OAuth2LoginResponseDTO> loginResult,
                                            String code, String state) {
        if (!R.isSuccess(loginResult) || loginResult.getData() == null) {
            // 登录失败，跳转到错误页面
            return buildErrorRedirectUrl(provider, loginResult != null ? loginResult.getMsg() : "登录失败");
        }

        OAuth2LoginResponseDTO response = loginResult.getData();
        OAuth2LoginResponseDTO.OAuth2LoginStatus status = response.getStatus();

        switch (status) {
            case SUCCESS:
                // 登录成功，跳转到主页
                return buildSuccessRedirectUrl(response);

            case NEED_SUPPLEMENT:
                // 需要补充信息，跳转到补充信息页面
                return buildSupplementRedirectUrl(provider, response, code, state);

            case NEED_BIND:
                // 需要绑定账号，跳转到绑定页面
                return buildBindRedirectUrl(provider, response, code, state);

            default:
                // 未知状态，跳转到错误页面
                return buildErrorRedirectUrl(provider, "未知的登录状态: " + status);
        }
    }

    /**
     * 构建登录成功重定向URL（跳转到主页）
     * 例如：http://localhost:8080 或 http://localhost:8080/home
     */
    private String buildSuccessRedirectUrl(OAuth2LoginResponseDTO response) {
        String homeUrl = oAuth2Properties.getFrontendHomeUrl();
        if (StringUtils.isBlank(homeUrl)) {
            // 如果未配置，使用默认值（请根据实际情况修改）
            homeUrl = "http://localhost:8080";
        }

        // 移除末尾的斜杠
        if (homeUrl.endsWith("/")) {
            homeUrl = homeUrl.substring(0, homeUrl.length() - 1);
        }

        // 构建URL参数，传递token等信息
        StringBuilder urlBuilder = new StringBuilder(homeUrl);
        urlBuilder.append("?oauth2=success");

        if (response.getLoginResponse() != null) {
            urlBuilder.append("&token=").append(URLEncoder.encode(
                    response.getLoginResponse().getAccessToken(), StandardCharsets.UTF_8));

            if (response.getLoginResponse().getRefreshToken() != null) {
                urlBuilder.append("&refreshToken=").append(URLEncoder.encode(
                        response.getLoginResponse().getRefreshToken(), StandardCharsets.UTF_8));
            }

            // 将完整的登录响应数据编码为JSON，通过URL参数传递（用于前端打印）
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                urlBuilder.append("&response=").append(URLEncoder.encode(responseJson, StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.warn("序列化登录响应失败", e);
            }
        }

        return urlBuilder.toString();
    }

    /**
     * 构建补充信息页面重定向URL
     * 例如：http://localhost:8080/oauth2/supplement
     */
    private String buildSupplementRedirectUrl(String provider, OAuth2LoginResponseDTO response,
                                              String code, String state) {
        String supplementUrl = oAuth2Properties.getFrontendSupplementUrl();
        if (StringUtils.isBlank(supplementUrl)) {
            // 如果未配置，使用默认值（请根据实际情况修改）
            supplementUrl = "http://localhost:8080/oauth2/supplement";
        }

        // 移除末尾的斜杠
        if (supplementUrl.endsWith("/")) {
            supplementUrl = supplementUrl.substring(0, supplementUrl.length() - 1);
        }

        // 构建URL参数，传递OAuth2用户信息和状态
        StringBuilder urlBuilder = new StringBuilder(supplementUrl);
        urlBuilder.append("?provider=").append(provider);
        urlBuilder.append("&code=").append(code);
        urlBuilder.append("&state=").append(state);
        urlBuilder.append("&status=").append(response.getStatus().name());

        // 将OAuth2用户信息编码为JSON，通过URL参数传递（用于前端打印和填充表单）
        if (response.getOauth2UserInfo() != null) {
            try {
                String userInfoJson = objectMapper.writeValueAsString(response.getOauth2UserInfo());
                urlBuilder.append("&oauth2UserInfo=").append(URLEncoder.encode(userInfoJson, StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.warn("序列化OAuth2用户信息失败", e);
            }
        }

        // 将完整的响应数据编码为JSON（用于前端打印）
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            urlBuilder.append("&response=").append(URLEncoder.encode(responseJson, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("序列化响应数据失败", e);
        }

        return urlBuilder.toString();
    }

    /**
     * 构建绑定页面重定向URL
     * 例如：http://localhost:8080/oauth2/bind
     */
    private String buildBindRedirectUrl(String provider, OAuth2LoginResponseDTO response,
                                        String code, String state) {
        String bindUrl = oAuth2Properties.getFrontendBindUrl();
        if (StringUtils.isBlank(bindUrl)) {
            // 如果未配置，使用默认值（请根据实际情况修改）
            bindUrl = "http://localhost:8080/oauth2/bind";
        }

        // 移除末尾的斜杠
        if (bindUrl.endsWith("/")) {
            bindUrl = bindUrl.substring(0, bindUrl.length() - 1);
        }

        // 构建URL参数，传递OAuth2用户信息和状态
        StringBuilder urlBuilder = new StringBuilder(bindUrl);
        urlBuilder.append("?provider=").append(provider);
        urlBuilder.append("&code=").append(code);
        urlBuilder.append("&state=").append(state);
        urlBuilder.append("&status=").append(response.getStatus().name());

        // 将OAuth2用户信息编码为JSON，通过URL参数传递（用于前端打印和填充表单）
        if (response.getOauth2UserInfo() != null) {
            try {
                String userInfoJson = objectMapper.writeValueAsString(response.getOauth2UserInfo());
                urlBuilder.append("&oauth2UserInfo=").append(URLEncoder.encode(userInfoJson, StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.warn("序列化OAuth2用户信息失败", e);
            }
        }

        // 将完整的响应数据编码为JSON（用于前端打印）
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            urlBuilder.append("&response=").append(URLEncoder.encode(responseJson, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("序列化响应数据失败", e);
        }

        return urlBuilder.toString();
    }

    /**
     * 构建错误页面重定向URL
     */
    private String buildErrorRedirectUrl(String provider, String errorMessage) {
        String errorUrl = oAuth2Properties.getFrontendErrorUrl();
        if (StringUtils.isBlank(errorUrl)) {
            // 如果未配置，使用默认值（请根据实际情况修改）
            errorUrl = "http://localhost:8080/oauth2/error";
        }

        // 移除末尾的斜杠
        if (errorUrl.endsWith("/")) {
            errorUrl = errorUrl.substring(0, errorUrl.length() - 1);
        }

        return errorUrl + "?provider=" + provider +
                "&status=ERROR&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
    }

    /**
     * 解绑OAuth2账号
     * 解除当前用户与指定第三方平台的绑定关系
     *
     * @param provider 第三方提供商（github, orcid等）
     * @return 操作结果
     */
    @PostMapping("/unbind/{provider}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "解绑OAuth2账号", description = "解除当前用户与指定第三方平台的绑定关系")
    public R<Void> unbindAccount(
            @Parameter(description = "OAuth2提供商名称", example = "github", required = true)
            @PathVariable String provider) {
        log.info("解绑OAuth2账号请求 - 提供商: {}", provider);

        try {
            // 从SecurityContext获取当前用户ID
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录，无法解绑");
            }

            R<Void> result = oAuth2Service.unbindAccount(userId, provider);
            return result;
        } catch (Exception e) {
            log.error("解绑OAuth2账号失败 - 提供商: {}, 错误: {}", provider, e.getMessage(), e);
            return R.fail("解绑失败: " + e.getMessage());
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
        try{
            // 1. 获取当前用户
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录");
            }

            // 2.查询用户的ORCID的绑定信息
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }
            User user = userOpt.get();
            String orcidId = user.getOrcidId();
            if (StringUtils.isBlank(orcidId)) {
                return R.fail("未获取到ORCID，可能是用户未绑定ORCID账号");
            }

            // 3. 直接检查 ORCID Provider 是否启用，不再依赖 oAuth2Client 的类型转换
            if (!orcidOAuth2Provider.isEnabled()) {
                return R.fail("后端配置未启用 ORCID 功能");
            }

            // 4. 这里需要ORCID的accessToken，因为ORCID的访问令牌20年有效期，所以说存储到数据库里就可以，如果失效了就重新授权获得ORCID的accessToken
            String accessToken = user.getOrcidAccessToken();
            // 如果 AccessToken 为空，可能需要处理（ORCID Token 有效期很长，通常数据库会有）
            if (StringUtils.isBlank(accessToken)) {
                return R.fail("未获取到有效的 ORCID 授权 Token，请尝试重新绑定账号");
            }

            // 5. 使用显式注入的 Provider 获取详细信息
            OrcidDetailDTO detail = orcidOAuth2Provider.getOrcidDetailInfo(orcidId, accessToken);

            log.info("成功获取ORCID详细信息 - 用户ID: {}, ORCID: {}", userId, orcidId);
            return R.ok(detail);
        }catch (OAuth2Exception e) {
            log.error("获取ORCID详细信息失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取ORCID详细信息异常", e);
            return R.fail("获取失败,请稍后重试");
        }
    }
}