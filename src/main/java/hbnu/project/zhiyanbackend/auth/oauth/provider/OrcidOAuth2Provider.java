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
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * ORCID OAuth2提供商实现
 * ORCID是用于唯一标识科研人员的国际标准
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
     * ORCID返回的token响应中包含orcid、id_token等额外信息
     * 根据ORCID OpenID Connect规范，token响应包含id_token（JWT），其中sub字段是ORCID iD
     */
    @Override
    public String getAccessToken(String code, String redirectUri) {
        String tokenUri = getTokenUri();
        String clientId = getClientId();
        String clientSecret = getClientSecret();

        if(StringUtils.isBlank(tokenUri) || StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
            throw new OAuth2Exception("OAuth2配置不完整：缺少tokenUri、clientId或clientSecret");
        }

        try {
            // ORCID要求使用form-urlencoded格式
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

            // 构建请求体
            String requestBody = String.format(
                    "client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                    clientId, clientSecret, code, redirectUri
            );
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if(response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                log.info("ORCID获取访问令牌响应状态: {}, 响应体长度: {}", 
                        response.getStatusCode(), body != null ? body.length() : 0);
                log.debug("ORCID获取访问令牌完整响应: {}", body);

                if (StringUtils.isBlank(body)) {
                    log.error("ORCID token响应为空");
                    throw new OAuth2Exception("ORCID token响应为空");
                }

                // 解析响应（使用Dict类型，因为JsonUtils.parseMap返回Dict）
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
            }else {
                String errorBody = response.getBody();
                log.error("获取ORCID访问令牌失败，状态码: {}, 响应体: {}", 
                        response.getStatusCode(), errorBody);
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
        try{
            // 首先从token中提取ORCID iD
            String orcidId = extractOrcidIdFromToken(accessToken);
            if (StringUtils.isEmpty(orcidId)) {
                throw new OAuth2Exception("无法从token中提取ORCID iD");
            }

            // 构建用户信息URL：https://pub.orcid.org/v3.0/{orcid-id}/person
            String userInfoUri = getUserInfoUri() + "/" + orcidId + "/person";

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> request = new HttpEntity<>(headers);
            // 发送
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
                // ORCID中用户信息需要公开才能拿到
                OAuth2UserInfoDTO userInfo = parseUserInfo(body);
                userInfo.setProviderUserId(orcidId);
                userInfo.setAccessToken(accessToken);

                return userInfo;
            } else {
                throw new OAuth2Exception("获取ORCID用户信息失败: " + response.getStatusCode());
            }
        }catch (Exception e){
            log.error("获取ORCID用户信息异常", e);
            throw new OAuth2Exception("获取用户信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析ORCID用户信息
     * ORCID的响应格式比较复杂，包含嵌套的XML风格的JSON
     * 需要用户公开自己的个人信息
     */
    @Override
    protected OAuth2UserInfoDTO parseUserInfo(String responseBody) {
        try{
            if (StringUtils.isBlank(responseBody)) {
                log.error("ORCID用户信息响应体为空");
                throw new OAuth2Exception("ORCID用户信息响应体为空");
            }
            
            log.debug("开始解析ORCID用户信息，响应体长度: {}", responseBody.length());
            
            // JsonUtils.parseMap返回Dict类型
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
                    if(StringUtils.isNotEmpty(givenName) && StringUtils.isNotEmpty(familyName)){
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
                    if(StringUtils.isNotEmpty(givenName) && StringUtils.isNotEmpty(familyName)){
                        fullName = givenName + " " + familyName;
                    } else if (StringUtils.isNotEmpty(givenName)) {
                        fullName = givenName;
                    } else if (StringUtils.isNotEmpty(familyName)) {
                        fullName = familyName;
                    }
                }
            }

            // 提取邮箱（ORCID的邮箱在emails字段中）
            // 需要用户设置为公开
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
                    // 将在getUserInfo中设置
                    .providerUserId("")
                    // ORCID没有username概念，使用全名
                    .username(fullName)
                    .nickname(fullName)
                    .email(email)
                    .build();
            log.info("解析ORCID用户信息成功: name={}, email={}", fullName, email);
            return userInfo;
        }catch (Exception e){
            log.error("解析ORCID用户信息失败", e);
            throw new OAuth2Exception("解析用户信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从token响应中提取ORCID iD
     * 根据ORCID OpenID Connect规范，优先从以下位置提取：
     * 1. token响应中的"orcid"字段（如果存在）
     * 2. id_token JWT中的"sub"字段（ORCID iD）
     * 3. 备选方案：使用token info endpoint
     */
    private String extractOrcidIdFromToken(String accessToken) {
        log.debug("开始提取ORCID iD，tokenResponse是否为null: {}", this.tokenResponse == null);
        
        // 方案1：从保存的token响应中提取
        if (this.tokenResponse != null) {
            log.debug("tokenResponse包含字段: {}", this.tokenResponse.keySet());
            
            // 优先从"orcid"字段获取
            String orcid = this.tokenResponse.getStr("orcid");
            if (StringUtils.isNotBlank(orcid)) {
                log.info("从token响应的orcid字段提取ORCID iD: {}", orcid);
                return orcid;
            } else {
                log.debug("token响应中未找到orcid字段");
            }

            // 从id_token JWT中提取
            String idToken = this.tokenResponse.getStr("id_token");
            if (StringUtils.isNotBlank(idToken)) {
                log.debug("找到id_token，长度: {}", idToken.length());
                try {
                    String orcidId = extractOrcidIdFromJwt(idToken);
                    if (StringUtils.isNotBlank(orcidId)) {
                        log.info("从id_token JWT提取ORCID iD: {}", orcidId);
                        return orcidId;
                    } else {
                        log.warn("从id_token JWT提取的ORCID iD为空");
                    }
                } catch (Exception e) {
                    log.warn("从id_token JWT提取ORCID iD失败: {}", e.getMessage(), e);
                }
            } else {
                log.debug("token响应中未找到id_token字段");
            }
        } else {
            log.warn("tokenResponse为null，无法从token响应中提取ORCID iD");
        }

        // 方案2：使用token info endpoint（备选方案）
        try {
            String tokenInfoUri = "https://orcid.org/oauth/tokeninfo";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String requestBody = "token=" + accessToken;
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenInfoUri,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Dict tokenInfo = JsonUtils.parseMap(response.getBody());
                if (tokenInfo != null) {
                    String orcid = tokenInfo.getStr("orcid");
                    if (StringUtils.isNotBlank(orcid)) {
                        log.debug("从token info endpoint提取ORCID iD: {}", orcid);
                        return orcid;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从token info endpoint提取ORCID iD失败: {}", e.getMessage());
        }

        log.error("无法从任何来源提取ORCID iD");
        return null;
    }

    /**
     * 从JWT id_token中提取ORCID iD
     * JWT格式：header.payload.signature
     * payload中的"sub"字段包含ORCID iD
     */
    private String extractOrcidIdFromJwt(String idToken) {
        try {
            if (StringUtils.isBlank(idToken)) {
                throw new OAuth2Exception("id_token为空");
            }

            log.debug("开始解析JWT id_token，长度: {}", idToken.length());

            // JWT由三部分组成，用.分隔
            String[] parts = idToken.split("\\.");
            log.debug("JWT分割后部分数量: {}", parts.length);
            
            if (parts.length != 3) {
                throw new OAuth2Exception("无效的JWT格式，期望3部分，实际: " + parts.length);
            }

            // 解码payload（第二部分）
            String payload = parts[1];
            if (StringUtils.isBlank(payload)) {
                throw new OAuth2Exception("JWT payload为空");
            }
            
            log.debug("JWT payload字符串长度: {}", payload.length());
            
            // Base64 URL解码
            byte[] decodedBytes = null;
            try {
                // 确保payload不为空再解码
                if (payload == null || payload.isEmpty()) {
                    log.error("JWT payload字符串为null或空");
                    throw new OAuth2Exception("JWT payload字符串为null或空");
                }
                
                // Base64 URL解码（不会返回null，但可能抛出异常）
                decodedBytes = Base64.getUrlDecoder().decode(payload);
                
                // Base64.getUrlDecoder().decode() 不会返回null，但为了安全起见还是检查
                if (decodedBytes == null) {
                    log.error("JWT payload Base64解码结果为null（不应该发生），payload: {}", payload);
                    throw new OAuth2Exception("JWT payload解码结果为null");
                }
                
            } catch (IllegalArgumentException e) {
                log.error("JWT payload Base64解码失败（格式错误），payload: {}", payload, e);
                throw new OAuth2Exception("JWT payload Base64解码失败: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("JWT payload Base64解码异常", e);
                throw new OAuth2Exception("JWT payload Base64解码异常: " + e.getMessage(), e);
            }

            // 再次检查解码结果（防御性编程）
            if (decodedBytes == null) {
                log.error("JWT payload Base64解码结果为null（第二次检查），payload: {}", payload);
                throw new OAuth2Exception("JWT payload解码结果为null");
            }

            // 安全地检查长度（先检查null再检查length）
            int length;
            try {
                length = decodedBytes.length;
            } catch (NullPointerException e) {
                log.error("访问decodedBytes.length时发生NullPointerException（不应该发生）", e);
                throw new OAuth2Exception("JWT payload解码结果异常: " + e.getMessage(), e);
            }
            
            if (length == 0) {
                log.error("JWT payload解码结果为空字节数组");
                throw new OAuth2Exception("JWT payload解码结果为空");
            }

            log.debug("JWT payload解码成功，字节数组长度: {}", length);

            String payloadJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            log.debug("JWT payload JSON字符串: {}", payloadJson);

            // 解析JSON获取sub字段
            Dict payloadMap = null;
            try {
                payloadMap = JsonUtils.parseMap(payloadJson);
            } catch (Exception e) {
                log.error("JWT payload JSON解析异常: {}", e.getMessage(), e);
                throw new OAuth2Exception("JWT payload JSON解析失败: " + e.getMessage(), e);
            }
            
            if (payloadMap == null) {
                log.error("JWT payload JSON解析结果为null，payloadJson: {}", payloadJson);
                throw new OAuth2Exception("JWT payload JSON解析失败：解析结果为null");
            }

            String sub = payloadMap.getStr("sub");
            if (StringUtils.isBlank(sub)) {
                log.error("JWT payload中未找到sub字段，可用字段: {}", payloadMap.keySet());
                throw new OAuth2Exception("JWT payload中未找到sub字段");
            }

            log.info("成功从JWT提取ORCID iD: {}", sub);
            return sub;
        } catch (OAuth2Exception e) {
            // 重新抛出OAuth2Exception
            throw e;
        } catch (Exception e) {
            log.error("从JWT提取ORCID iD失败", e);
            throw new OAuth2Exception("从JWT提取ORCID iD失败: " + e.getMessage(), e);
        }
    }
}
