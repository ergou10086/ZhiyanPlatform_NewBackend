package hbnu.project.zhiyanbackend.auth.service;

import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.basic.domain.R;
import java.util.List;

/**
 * OAuth2第三方登录服务接口
 * 处理OAuth2登录、用户绑定等业务逻辑
 *
 * @author ErgouTree
 * @rewrite yui
 */
public interface OAuth2Service {

    /**
     * 处理 OAuth2 登录/注册
     *
     * 核心逻辑：
     * 1. 先查 UserConnection 表，看这个第三方账号是否已绑定
     * 2. 如果已绑定 -> 直接登录
     * 3. 如果未绑定：
     *    a. 尝试邮箱匹配（如果有邮箱）
     *    b. 匹配成功 -> 自动绑定 + 登录
     *    c. 匹配失败 -> 静默注册新账号 + 自动绑定 + 登录
     *
     * @param oauth2UserInfo OAuth2 提供商返回的用户信息
     * @return 登录响应
     */
    R<OAuth2LoginResponseDTO> handleOAuth2Login(OAuth2UserInfoDTO oauth2UserInfo);

    /**
     * 已登录用户手动绑定第三方账号
     *
     * 场景：用户在个人中心点击"绑定 GitHub"
     *
     * 验证逻辑：
     * 1. 检查该第三方账号是否已被其他用户绑定
     * 2. 检查当前用户是否已绑定过该提供商
     * 3. 验证通过后创建绑定关系
     *
     * @param userId         当前登录用户ID
     * @param oauth2UserInfo OAuth2 用户信息
     * @return 绑定结果
     */
    R<Void> bindOAuth2Account(Long userId, OAuth2UserInfoDTO oauth2UserInfo);

    /**
     * 解绑第三方账号
     *
     * 安全检查：
     * 1. 如果用户没有密码，且只剩一个绑定，不允许解绑（会导致无法登录）
     * 2. 否则允许解绑
     *
     * @param userId   用户ID
     * @param provider 提供商名称
     * @return 解绑结果
     */
    R<Void> unbindOAuth2Account(Long userId, String provider);

    /**
     * 查询用户的所有绑定关系
     *
     * @param userId 用户ID
     * @return 绑定关系列表
     */
    R<List<UserConnectionDTO>> getUserConnections(Long userId);

    /**
     * 检查第三方账号是否已被绑定
     *
     * @param provider       提供商名称
     * @param providerUserId 提供商用户ID
     * @return 是否已绑定
     */
    boolean isOAuth2AccountBound(String provider, String providerUserId);

    /**
     * 获取ORCID用户详细信息
     *
     * @param userId 用户ID
     * @return ORCID详细信息
     */
    R<OrcidDetailDTO> getOrcidDetail(Long userId);
}

