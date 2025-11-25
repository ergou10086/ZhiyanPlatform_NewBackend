package hbnu.project.zhiyanbackend.auth.oauth.client;

import hbnu.project.zhiyanbackend.auth.model.dto.AuthorizationResultDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2UserInfoDTO;
import org.springframework.stereotype.Component;

/**
 * OAuth2客户端接口
 * 封装OAuth2登录流程的核心逻辑
 *
 * @author ErgouTree
 */
@Component
public interface OAuth2Client {

    /**
     * 生成授权URL
     *
     * @param providerName 提供商名称（如：github）
     * @param redirectUri  回调地址
     * @return 授权结果（包含授权URL和state）
     */
    AuthorizationResultDTO getAuthorizationUrl(String providerName, String redirectUri);

    /**
     * 通过授权码获取用户信息
     *
     * @param providerName 提供商名称
     * @param code         授权码
     * @param state        状态参数
     * @param redirectUri  回调地址
     * @return 用户信息
     */
    OAuth2UserInfoDTO getUserInfoByCode(String providerName, String code, String state, String redirectUri);
}
