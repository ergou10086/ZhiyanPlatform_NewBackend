package hbnu.project.zhiyanbackend.auth.oauth.provider;

import cn.hutool.core.lang.Dict;
import hbnu.project.zhiyanbackend.auth.oauth.config.properties.OAuth2Properties;
import hbnu.project.zhiyanbackend.auth.exeption.OAuth2Exception;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2UserInfoDTO;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * ORCID OAuth2提供商实现
 * ORCID是用于唯一标识科研人员的国际标准
 *
 * Token请求必须使用 application/x-www-form-urlencoded 格式，且client_id 和 client_secret 必须作为请求体参数
 * 不使用 Basic Auth，使用表单
 *
 *
 * @author ErgouTree
 */
@Slf4j
@Component
public class OrcidOAuth2Provider extends AbstractOAuth2Provider{

    private static final String PROVIDER_NAME = "orcid";
    
    /**
     * 保存token响应，用于提取ORCID iD
     */
    private Dict tokenResponse;

    public OrcidOAuth2Provider(OAuth2Properties properties, RestTemplate restTemplate) {
        super(properties, restTemplate);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isEnabled() {
        return properties.getOrcid().isEnabled();
    }

    @Override
    protected String getClientId() {
        return properties.getOrcid().getClientId();
    }

    @Override
    protected String getClientSecret() {
        return properties.getOrcid().getClientSecret();
    }

    @Override
    protected String getScope() {
        return properties.getOrcid().getScope();
    }

    @Override
    protected String getAuthorizationUri() {
        return properties.getOrcid().getAuthorizationUri();
    }

    @Override
    protected String getTokenUri() {
        return properties.getOrcid().getTokenUri();
    }

    @Override
    protected String getUserInfoUri() {
        return properties.getOrcid().getUserInfoUri();
    }

    /**
     * 重写getAccessToken以处理ORCID的特殊响应格式
     * 必须使用 application/x-www-form-urlencoded，client_id 和 client_secret 作为表单参数
     * 不使用 HTTP Basic Auth
     */
    @Override
    public String getAccessToken(String code, String redirectUri) {
        String tokenUri = getTokenUri();
        String clientId = getClientId();
        String clientSecret = getClientSecret();

        if (StringUtils.isBlank(tokenUri) || StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
            throw new OAuth2Exception("OAuth2配置不完整：缺少tokenUri、clientId或clientSecret");
        }

        try {
            log.debug("ORCID token请求 - clientId: {}, redirectUri: {}, code长度: {}",
                    clientId, redirectUri, code != null ? code.length() : 0);

            // 检查client_secret是否为空
            if (StringUtils.isBlank(clientSecret)) {
                log.error("ORCID client_secret为空，请检查配置");
                throw new OAuth2Exception("ORCID client_secret未配置，请检查配置文件中的 zhiyan.oauth2.orcid.client-secret 或环境变量 ORCID_CLIENT_SECRET");
            }

            // 构建请求体 - ORCID 要求使用表单参数方式
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("redirect_uri", redirectUri);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

            log.debug("ORCID token请求头: Content-Type=application/x-www-form-urlencoded, Accept=application/json");
            log.debug("ORCID token请求体参数: grant_type=authorization_code, code={}, redirect_uri={}", code, redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                log.info("ORCID获取访问令牌响应状态: {}, 响应体长度: {}", response.getStatusCode(), body != null ? body.length() : 0);
                log.debug("ORCID获取访问令牌完整响应: {}", body);

                if (StringUtils.isBlank(body)) {
                    log.error("ORCID token响应为空");
                    throw new OAuth2Exception("ORCID token响应为空");
                }

                // 解析响应（使用Dict类型）
                Dict tokenMap = null;
                try {
                    tokenMap = JsonUtils.parseMap(body);
                } catch (Exception e) {
                    log.error("ORCID token响应JSON解析异常: {}", e.getMessage(), e);
                    throw new OAuth2Exception("ORCID token响应JSON解析失败: " + e.getMessage(), e);
                }

                if (tokenMap == null) {
                    log.error("ORCID token响应解析结果为null，响应体: {}", body);
                    throw new OAuth2Exception("ORCID token响应解析失败：解析结果为null");
                }

                // 保存token响应，用于后续提取ORCID iD
                this.tokenResponse = tokenMap;
                log.debug("ORCID token响应解析成功，包含字段: {}", tokenMap.keySet());

                String accessToken = tokenMap.getStr("access_token");
                if (StringUtils.isEmpty(accessToken)) {
                    log.error("ORCID响应中未找到access_token，可用字段: {}", tokenMap.keySet());
                    throw new OAuth2Exception("ORCID响应中未找到access_token");
                }

                log.info("成功获取ORCID access_token，长度: {}", accessToken.length());
                return accessToken;
            } else {
                String errorBody = response.getBody();
                int statusCode = response.getStatusCode().value();
                log.error("获取ORCID访问令牌失败，状态码: {}, 响应体: {}", statusCode, errorBody);
                throw new OAuth2Exception("获取ORCID访问令牌失败: " + response.getStatusCode() +
                        (errorBody != null ? ", " + errorBody : ""));
            }
        } catch (Exception e) {
            log.error("获取ORCID访问令牌异常", e);
            throw new OAuth2Exception("获取访问令牌失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重写getUserInfo以处理ORCID的API调用
     * ORCID需要使用ORCID iD来构建API URL
     */
    @Override
    public OAuth2UserInfoDTO getUserInfo(String accessToken) {
        try {
            // 首先从token中提取ORCID iD
            String orcidId = extractOrcidIdFromToken(accessToken);
            if (StringUtils.isEmpty(orcidId)) {
                throw new OAuth2Exception("无法从token中提取ORCID iD");
            }

            // 构建用户信息URL：https://pub.orcid.org/v3.0/{orcid-id}/person
            String userInfoUri = getUserInfoUri() + "/" + orcidId + "/person";
            log.debug("ORCID用户信息请求URL: {}", userInfoUri);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String body = response.getBody();
                log.debug("ORCID获取用户信息响应: {}", body);

                // 解析用户信息
                OAuth2UserInfoDTO userInfo = parseUserInfo(body);
                userInfo.setProviderUserId(orcidId);
                userInfo.setAccessToken(accessToken);

                return userInfo;
            } else {
                throw new OAuth2Exception("获取ORCID用户信息失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取ORCID用户信息异常", e);
            throw new OAuth2Exception("获取用户信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析ORCID用户信息
     * ORCID的响应格式比较复杂，包含嵌套的XML风格的JSON
     */
    @Override
    protected OAuth2UserInfoDTO parseUserInfo(String responseBody) {
        try {
            if (StringUtils.isBlank(responseBody)) {
                log.error("ORCID用户信息响应体为空");
                throw new OAuth2Exception("ORCID用户信息响应体为空");
            }

            log.debug("开始解析ORCID用户信息，响应体长度: {}", responseBody.length());

            Dict personData = null;
            try {
                personData = JsonUtils.parseMap(responseBody);
            } catch (Exception e) {
                log.error("ORCID用户信息JSON解析异常: {}", e.getMessage(), e);
                throw new OAuth2Exception("ORCID用户信息JSON解析失败: " + e.getMessage(), e);
            }

            if (personData == null) {
                log.error("ORCID用户信息解析结果为null，响应体: {}", responseBody);
                throw new OAuth2Exception("ORCID用户信息解析失败：解析结果为null");
            }

            log.debug("ORCID用户信息解析成功，包含字段: {}", personData.keySet());

            // 提取用户名
            String givenName = null;
            String familyName = null;
            String fullName = null;

            if (personData.containsKey("name")) {
                Object nameObj = personData.get("name");
                if (nameObj instanceof Dict nameDict) {
                    // 提取given-names
                    Object givenNamesObj = nameDict.get("given-names");
                    if (givenNamesObj instanceof Dict givenNamesDict) {
                        givenName = givenNamesDict.getStr("value");
                    }
                    // 提取family-name
                    Object familyNameObj = nameDict.get("family-name");
                    if (familyNameObj instanceof Dict familyNameDict) {
                        familyName = familyNameDict.getStr("value");
                    }

                    // 构建全名
                    if (StringUtils.isNotEmpty(givenName) && StringUtils.isNotEmpty(familyName)) {
                        fullName = givenName + " " + familyName;
                    } else if (StringUtils.isNotEmpty(givenName)) {
                        fullName = givenName;
                    } else if (StringUtils.isNotEmpty(familyName)) {
                        fullName = familyName;
                    }
                } else if (nameObj instanceof Map) {
                    // 兼容Map类型
                    Map<String, Object> nameMap = (Map<String, Object>) nameObj;
                    Object givenNamesObj = nameMap.get("given-names");
                    if (givenNamesObj instanceof Map) {
                        Map<String, Object> givenNamesMap = (Map<String, Object>) givenNamesObj;
                        givenName = (String) givenNamesMap.get("value");
                    }
                    Object familyNameObj = nameMap.get("family-name");
                    if (familyNameObj instanceof Map) {
                        Map<String, Object> familyNameMap = (Map<String, Object>) familyNameObj;
                        familyName = (String) familyNameMap.get("value");
                    }

                    // 构建全名
                    if (StringUtils.isNotEmpty(givenName) && StringUtils.isNotEmpty(familyName)) {
                        fullName = givenName + " " + familyName;
                    } else if (StringUtils.isNotEmpty(givenName)) {
                        fullName = givenName;
                    } else if (StringUtils.isNotEmpty(familyName)) {
                        fullName = familyName;
                    }
                }
            }

            // 提取邮箱（ORCID的邮箱在emails字段中）
            String email = null;
            if (personData.containsKey("emails")) {
                Object emailsObj = personData.get("emails");
                if (emailsObj != null) {
                    Object emailListObj = null;
                    if (emailsObj instanceof Dict) {
                        emailListObj = ((Dict) emailsObj).get("email");
                    } else if (emailsObj instanceof Map) {
                        emailListObj = ((Map<String, Object>) emailsObj).get("email");
                    }

                    if (emailListObj instanceof List<?> emailList) {
                        // 优先获取primary和verified的邮箱
                        for (Object emailItemObj : emailList) {
                            if (emailItemObj instanceof Dict emailItem) {
                                Boolean primary = emailItem.getBool("primary");
                                Boolean verified = emailItem.getBool("verified");
                                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                                    email = emailItem.getStr("email");
                                    break;
                                }
                            } else if (emailItemObj instanceof Map) {
                                Map<String, Object> emailItem = (Map<String, Object>) emailItemObj;
                                Boolean primary = (Boolean) emailItem.get("primary");
                                Boolean verified = (Boolean) emailItem.get("verified");
                                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                                    email = (String) emailItem.get("email");
                                    break;
                                }
                            }
                        }
                        // 如果没有primary和verified，获取第一个primary的
                        if (email == null) {
                            for (Object emailItemObj : emailList) {
                                if (emailItemObj instanceof Dict emailItem) {
                                    Boolean primary = emailItem.getBool("primary");
                                    if (Boolean.TRUE.equals(primary)) {
                                        email = emailItem.getStr("email");
                                        break;
                                    }
                                } else if (emailItemObj instanceof Map) {
                                    Map<String, Object> emailItem = (Map<String, Object>) emailItemObj;
                                    Boolean primary = (Boolean) emailItem.get("primary");
                                    if (Boolean.TRUE.equals(primary)) {
                                        email = (String) emailItem.get("email");
                                        break;
                                    }
                                }
                            }
                        }
                        // 如果还没有，获取第一个邮箱
                        if (email == null && !emailList.isEmpty()) {
                            Object firstEmail = emailList.getFirst();
                            if (firstEmail instanceof Dict) {
                                email = ((Dict) firstEmail).getStr("email");
                            } else if (firstEmail instanceof Map) {
                                email = (String) ((Map<String, Object>) firstEmail).get("email");
                            }
                        }
                    }
                }
            }

            // 构建OAuth2UserInfoDTO
            OAuth2UserInfoDTO userInfo = OAuth2UserInfoDTO.builder()
                    .provider("orcid")
                    .providerUserId("") // 将在getUserInfo中设置
                    .username(fullName)
                    .nickname(fullName)
                    .email(email)
                    .build();
            log.info("解析ORCID用户信息成功: name={}, email={}", fullName, email);
            return userInfo;
        } catch (Exception e) {
            log.error("解析ORCID用户信息失败", e);
            throw new OAuth2Exception("解析用户信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从token响应中提取ORCID iD
     * ORCID在token响应中直接返回orcid字段
     */
    private String extractOrcidIdFromToken(String accessToken) {
        log.debug("开始提取ORCID iD，tokenResponse是否为null: {}", this.tokenResponse == null);

        // 方案1：从保存的tokenResponse中提取
        if (this.tokenResponse != null) {
            log.debug("tokenResponse包含字段: {}", this.tokenResponse.keySet());

            // ORCID在token响应中直接返回orcid字段
            String orcid = this.tokenResponse.getStr("orcid");
            if (StringUtils.isNotBlank(orcid)) {
                log.info("从token响应的orcid字段提取ORCID iD: {}", orcid);
                return orcid;
            } else {
                log.debug("token响应中未找到orcid字段");
            }

            // 备选：从id_token JWT中提取
            String idToken = this.tokenResponse.getStr("id_token");
            if (StringUtils.isNotBlank(idToken)) {
                log.debug("找到id_token，长度: {}", idToken.length());
                try {
                    String orcidId = extractOrcidIdFromJwt(idToken);
                    if (StringUtils.isNotBlank(orcidId)) {
                        log.info("从id_token JWT提取ORCID iD: {}", orcidId);
                        return orcidId;
                    }
                } catch (Exception e) {
                    log.warn("从id_token JWT提取ORCID iD失败: {}", e.getMessage());
                }
            }
        } else {
            log.warn("tokenResponse为null，无法从token响应中提取ORCID iD");
        }

        log.error("无法从任何来源提取ORCID iD");
        return null;
    }

    /**
     * 从JWT id_token中提取ORCID iD
     * JWT格式：header.payload.signature
     */
    private String extractOrcidIdFromJwt(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new OAuth2Exception("无效的JWT格式");
            }

            // 解码payload（第二部分）
            String payload = parts[1];
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);

            // 解析JSON获取sub字段（ORCID iD）
            Dict payloadMap = JsonUtils.parseMap(payloadJson);
            if (payloadMap == null) {
                throw new OAuth2Exception("JWT payload解析失败");
            }

            String sub = payloadMap.getStr("sub");
            if (StringUtils.isBlank(sub)) {
                throw new OAuth2Exception("JWT payload中未找到sub字段");
            }

            return sub;
        } catch (Exception e) {
            log.error("从JWT提取ORCID iD失败", e);
            throw new OAuth2Exception("从JWT提取ORCID iD失败: " + e.getMessage(), e);
        }
    }
}
