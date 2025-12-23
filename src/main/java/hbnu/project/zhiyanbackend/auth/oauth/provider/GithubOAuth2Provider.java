package hbnu.project.zhiyanbackend.auth.oauth.provider;

import cn.hutool.core.lang.Dict;
import hbnu.project.zhiyanbackend.auth.exeption.OAuth2Exception;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2UserInfoDTO;
import hbnu.project.zhiyanbackend.auth.oauth.config.properties.OAuth2Properties;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GitHub OAuth2 提供商实现
 * 参考文档：
 * - https://docs.github.com/zh/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps
 * - https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html#oauth2login-core-registration-github
 *
 * @author ErgouTree
 */
@Slf4j
@Component
public class GithubOAuth2Provider extends AbstractOAuth2Provider {

    private static final String PROVIDER_NAME = "github";

    public GithubOAuth2Provider(OAuth2Properties properties, RestTemplate restTemplate) {
        super(properties, restTemplate);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isEnabled() {
        return properties.isGithub_enabled();
    }

    @Override
    protected String getClientId() {
        return properties.getGithub_clientId();
    }

    @Override
    protected String getClientSecret() {
        return properties.getGithub_clientSecret();
    }

    @Override
    protected String getScope() {
        return properties.getGithub_scope();
    }

    @Override
    protected String getAuthorizationUri() {
        return properties.getGithub_authorizationUri();
    }

    @Override
    protected String getTokenUri() {
        return properties.getGithub_tokenUri();
    }

    @Override
    protected String getUserInfoUri() {
        return properties.getGithub_userInfoUri();
    }

    /**
     * 解析 GitHub 用户信息
     * GitHub API 返回的用户信息结构：
     * {
     *   "login": "octocat",
     *   "id": 1,
     *   "avatar_url": "https://github.com/images/error/octocat_happy.gif",
     *   "name": "The Octocat",
     *   "email": "octocat@github.com",  // 可能为 null
     *   "bio": "There once was...",
     *   ...
     * }
     */
    @Override
    protected OAuth2UserInfoDTO parseUserInfo(String responseBody) {
        try{
            if(StringUtils.isBlank(responseBody)){
                log.error("GitHub 用户信息响应体为空");
                throw new OAuth2Exception("GitHub 用户信息响应体为空");
            }

            log.debug("开始解析 GitHub 用户信息，响应体长度: {}", responseBody.length());

            Dict userData = JsonUtils.parseMap(responseBody);
            if (userData == null) {
                log.error("GitHub 用户信息解析结果为 null，响应体: {}", responseBody);
                throw new OAuth2Exception("GitHub 用户信息解析失败：解析结果为 null");
            }

            log.debug("GitHub 用户信息解析成功，包含字段: {}", userData.keySet());

            // 提取用户基本信息，只提取需要的
            String login = userData.getStr("login");  // GitHub 用户名（必有）
            Long id = userData.getLong("id");         // GitHub 用户 ID（必有）
            String email = userData.getStr("email");  // 邮箱（可能为 null，需要用户公开）
            if (StringUtils.isBlank(login) || id == null) {
                throw new OAuth2Exception("GitHub 响应缺少必需字段：login 或 id");
            }

            // 使用 login 作为 providerUserId（GitHub 官方推荐使用 id，但 login 更直观）
            // 这里使用 id 的字符串形式，因为它是不可变的
            String providerUserId = String.valueOf(id);

            // 构建 OAuth2UserInfoDTO
            OAuth2UserInfoDTO userInfo = OAuth2UserInfoDTO.builder()
                    .provider(PROVIDER_NAME)
                    .providerUserId(providerUserId)
                    .username(login)
                    // 使用 login 作为昵称
                    .nickname(login)
                    // 可能为 null，后续需要通过 /user/emails 获取
                    .email(email)
                    .build();

            log.info("解析 GitHub 用户信息成功: login={}, id={}, name={}, email={}", login, id, login, email);

            return userInfo;
        }catch (OAuth2Exception e){
            log.error("解析 GitHub 用户信息失败", e);
            throw new OAuth2Exception("解析用户信息失败: " + e.getMessage(), e);
        }
    }


    /**
     * 重写 getUserInfo 以处理 GitHub 的特殊情况
     * GitHub 的 /user 接口返回的 email 可能为 null（当用户设置邮箱为私有时）
     * 此时需要调用 /user/emails 接口获取邮箱
     */
    @Override
    public OAuth2UserInfoDTO getUserInfo(String accessToken) {
        try{
            // 1. 获取用户的基本信息
            OAuth2UserInfoDTO userInfoDTO = super.getUserInfo(accessToken);

            // 2. 如果邮箱为空，尝试从 /user/emails 获取
            if (StringUtils.isBlank(userInfoDTO.getEmail())) {
                log.info("用户未公开邮箱，尝试从 /user/emails 获取");
                String primaryEmail = fetchPrimaryEmail(accessToken);
                if (StringUtils.isNotBlank(primaryEmail)) {
                    userInfoDTO.setEmail(primaryEmail);
                    log.info("成功从 /user/emails 获取主邮箱: {}", primaryEmail);
                } else {
                    log.warn("无法获取用户邮箱，用户可能未验证邮箱或未授权 user:email scope");
                }
            }

            // 3. 设置 accessToken
            userInfoDTO.setAccessToken(accessToken);

            return userInfoDTO;
        } catch (Exception e) {
            log.error("获取 GitHub 用户信息异常", e);
            throw new OAuth2Exception("获取用户信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户的主邮箱
     * GitHub /user/emails API 返回用户所有邮箱，格式：
     * [
     *   {
     *     "email": "octocat@github.com",
     *     "primary": true,
     *     "verified": true,
     *     "visibility": "public"
     *   },
     *   ...
     * ]
     */
    private String fetchPrimaryEmail(String accessToken) {
        String emailUri = properties.getGithub_userEmailUri();

        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            // GitHub API 要求设置 User-Agent
            headers.set("User-Agent", "Zhiyan-Platform");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    emailUri,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String body = response.getBody();
                log.debug("获取 GitHub 邮箱列表响应: {}", body);

                // 解析邮箱列表
                List<Map<String, Object>> emails = JsonUtils.parseMapList(body);
                if (emails != null && !emails.isEmpty()) {
                    // 优先获取 primary 且 verified 的邮箱
                    for (Map<String, Object> emailItem : emails) {
                        Boolean primary = (Boolean) emailItem.get("primary");
                        Boolean verified = (Boolean) emailItem.get("verified");
                        if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                            return (String) emailItem.get("email");
                        }
                    }
                    // 如果没有 primary 且 verified 的，获取第一个 verified 的
                    for (Map<String, Object> emailItem : emails) {
                        Boolean verified = (Boolean) emailItem.get("verified");
                        if (Boolean.TRUE.equals(verified)) {
                            return (String) emailItem.get("email");
                        }
                    }
                    // 如果都没有，返回第一个
                    return (String) emails.getFirst().get("email");
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("获取 GitHub 邮箱失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 重写 getAccessToken 以处理 GitHub 的特殊响应格式
     * GitHub 默认返回 application/x-www-form-urlencoded 格式，需要在请求头中指定 Accept: application/json
     */
    @Override
    public String getAccessToken(String code, String redirectUri) {
        String tokenUri = getTokenUri();
        String clientId = getClientId();
        String clientSecret = getClientSecret();

        if (StringUtils.isBlank(tokenUri) || StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
            throw new OAuth2Exception("OAuth2 配置不完整：缺少 tokenUri、clientId 或 clientSecret");
        }

        try {
            log.debug("GitHub token 请求 - clientId: {}, redirectUri: {}, code 长度: {}",
                    clientId, redirectUri, code != null ? code.length() : 0);

            // 构建请求参数（使用 JSON 格式）
            Map<String, String> params = Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "code", code,
                    "redirect_uri", redirectUri
            );

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 关键：必须指定 Accept: application/json，否则 GitHub 返回 form-urlencoded 格式
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Zhiyan-Platform");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();

                if (StringUtils.isBlank(body)) {
                    log.error("GitHub token 响应为空");
                    throw new OAuth2Exception("GitHub token 响应为空");
                }

                log.debug("GitHub token 响应: {}", body);

                // 解析响应
                Dict tokenData = JsonUtils.parseMap(body);
                if (tokenData == null) {
                    throw new OAuth2Exception("GitHub token 响应解析失败");
                }

                String accessToken = tokenData.getStr("access_token");
                if (StringUtils.isEmpty(accessToken)) {
                    log.error("GitHub 响应中未找到 access_token，可用字段: {}", tokenData.keySet());
                    // 检查是否有错误信息
                    String error = tokenData.getStr("error");
                    String errorDescription = tokenData.getStr("error_description");
                    if (StringUtils.isNotBlank(error)) {
                        throw new OAuth2Exception("GitHub 返回错误: " + error +
                                (StringUtils.isNotBlank(errorDescription) ? " - " + errorDescription : ""));
                    }
                    throw new OAuth2Exception("GitHub 响应中未找到 access_token");
                }

                log.info("成功获取 GitHub access_token，长度: {}", accessToken.length());
                return accessToken;
            } else {
                String errorBody = response.getBody();
                int statusCode = response.getStatusCode().value();
                log.error("获取 GitHub 访问令牌失败，状态码: {}, 响应体: {}", statusCode, errorBody);
                throw new OAuth2Exception("获取 GitHub 访问令牌失败: " + response.getStatusCode() +
                        (errorBody != null ? ", " + errorBody : ""));
            }
        } catch (Exception e) {
            log.error("获取 GitHub 访问令牌异常", e);
            throw new OAuth2Exception("获取访问令牌失败: " + e.getMessage(), e);
        }
    }
}
