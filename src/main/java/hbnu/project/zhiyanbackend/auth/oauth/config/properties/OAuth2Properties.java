package hbnu.project.zhiyanbackend.auth.oauth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 总配置属性
 * 关联所有第三方提供商的配置
 *
 * @author ErgouTree
 */
@Data
@ConfigurationProperties(prefix = "zhiyan.oauth2")
public class OAuth2Properties {

    /**
     * 是否启用OAuth2功能
     */
    private boolean enabled = true;

    /**
     * 回调地址基础路径（不包含具体路径）
     */
    private String callbackBaseUrl;

    /**
     * 前端主页URL（登录成功后跳转）
     */
    private String frontendHomeUrl;

    /**
     * 前端错误页面URL（登录失败时跳转）
     */
    private String frontendErrorUrl;

    // -------- ORCID 配置 ------

    /**
     * 是否启用ORCID登录
     */
    private boolean orcid_enabled = true;

    /**
     * ORCID 应用的 Client ID（从ORCID开发者平台获取）
     */
    private String orcid_clientId;

    /**
     * ORCID 应用的 Client Secret（从ORCID开发者平台获取）
     */
    private String orcid_clientSecret;

    /**
     * 授权范围（scope）
     * ORCID支持的scope：
     * - /authenticate: 只获取ORCID iD
     * - /read-limited: 读取受限信息（教育、就业、研究资源等）
     * - /person/update: 更新个人信息
     * - /activities/update: 更新活动信息
     *
     * 默认使用 /authenticate openid 以获取基本身份信息
     */
    private String orcid_scope = "/authenticate openid";

    /**
     * ORCID 授权服务器的授权端点URL
     * 生产环境：https://orcid.org/oauth/authorize
     * 沙盒环境：https://sandbox.orcid.org/oauth/authorize
     */
    private String orcid_authorizationUri = "https://orcid.org/oauth/authorize";

    /**
     * ORCID 授权服务器的令牌端点URL（用于通过授权码换令牌）
     * 生产环境：https://orcid.org/oauth/token
     * 沙盒环境：https://sandbox.orcid.org/oauth/token
     */
    private String orcid_tokenUri = "https://orcid.org/oauth/token";

    /**
     * ORCID 用户信息API端点URL（用于获取用户信息）
     * 使用 ORCID iD 构建：https://pub.orcid.org/v3.0/{orcid-id}/person
     * 这个URL在运行时动态构建，此处配置基础URL
     */
    private String orcid_userInfoUri = "https://pub.orcid.org/v3.0";

    /**
     * ORCID API版本
     */
    private String orcid_apiVersion = "v3.0";

    // --------  Github 配置  ---------

    /**
     * 是否启用 GitHub 登录
     */
    private boolean github_enabled = true;

    /**
     * GitHub OAuth App 的 Client ID
     */
    private String github_clientId;

    /**
     * GitHub OAuth App 的 Client Secret
     */
    private String github_clientSecret;

    /**
     * 授权范围（scope）
     * GitHub 支持的 scope：
     * - read:user: 读取用户基本信息
     * - user:email: 读取用户邮箱（包括私有邮箱）
     * - read:org: 读取用户所属组织
     *
     * 默认使用 read:user user:email 以获取用户基本信息和邮箱
     */
    private String github_scope = "read:user user:email";

    /**
     * GitHub 授权服务器的授权端点 URL
     * 标准端点：https://github.com/login/oauth/authorize
     */
    private String github_authorizationUri = "https://github.com/login/oauth/authorize";

    /**
     * GitHub 授权服务器的令牌端点 URL（用于通过授权码换取令牌）
     * 标准端点：https://github.com/login/oauth/access_token
     */
    private String github_tokenUri = "https://github.com/login/oauth/access_token";

    /**
     * GitHub 用户信息 API 端点 URL
     * 标准端点：https://api.github.com/user
     */
    private String github_userInfoUri = "https://api.github.com/user";

    /**
     * GitHub 用户邮箱 API 端点 URL
     * 标准端点：https://api.github.com/user/emails
     * 用于获取用户的主邮箱（包括私有邮箱）
     */
    private String github_userEmailUri = "https://api.github.com/user/emails";
}