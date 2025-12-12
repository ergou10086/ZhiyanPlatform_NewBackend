package hbnu.project.zhiyanbackend.auth.oauth.config.properties;

import lombok.Data;

/**
 * ORCID 平台的 OAuth2 配置属性
 * ORCID是用于唯一标识科研人员的国际标准
 *
 * @author ErgouTree
 */
@Data
public class OrcidOAuthProperties {

    /**
     * 是否启用ORCID登录
     */
    private boolean enabled = false;

    /**
     * ORCID 应用的 Client ID（从ORCID开发者平台获取）
     */
    private String clientId;

    /**
     * ORCID 应用的 Client Secret（从ORCID开发者平台获取）
     */
    private String clientSecret;

    /**
     * 授权范围（scope）
     * ORCID支持的scope：
     * - /authenticate: 只获取ORCID iD（旧方式，不推荐）
     * - openid: OpenID Connect认证，获取ORCID iD和id_token（推荐）
     * - /read-limited: 读取公开信息（教育、就业、研究资源、关键词等）
     * - /person/update: 更新个人信息
     * - /activities/update: 更新活动信息
     * 
     * 注意：/authenticate 和 openid 不能同时使用，它们具有相同的授权
     * 推荐使用 openid /read-limited 以获取身份信息和公开数据
     */
    private String scope = "openid /read-limited";

    /**
     * ORCID 授权服务器的授权端点URL
     * 生产环境：https://orcid.org/oauth/authorize
     * 沙盒环境：https://sandbox.orcid.org/oauth/authorize
     */
    private String authorizationUri = "https://orcid.org/oauth/authorize";

    /**
     * ORCID 授权服务器的令牌端点URL（用于通过授权码换令牌）
     * 生产环境：https://orcid.org/oauth/token
     * 沙盒环境：https://sandbox.orcid.org/oauth/token
     */
    private String tokenUri = "https://orcid.org/oauth/token";

    /**
     * ORCID 用户信息API端点URL（用于获取用户信息）
     * 使用 ORCID iD 构建：https://pub.orcid.org/v3.0/{orcid-id}/person
     * 这个URL在运行时动态构建，此处配置基础URL
     */
    private String userInfoUri = "https://pub.orcid.org/v3.0";

    /**
     * ORCID API版本
     */
    private String apiVersion = "v3.0";

//    /**
//     * 请求ORCID API时需要携带的头信息
//     * 官方要求必须包含Accept和User-Agent等头信息
//     */
//    private Map<String, String> requestHeaders = new HashMap<>();
}
