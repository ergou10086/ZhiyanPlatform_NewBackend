package hbnu.project.zhiyanbackend.auth.service;

import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2BindAccountDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2LoginResponseDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2SupplementInfoDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2UserInfoDTO;
import hbnu.project.zhiyanbackend.basic.domain.R;

/**
 * OAuth2第三方登录服务接口
 * 处理OAuth2登录、用户绑定等业务逻辑
 *
 * @author ErgouTree
 * @rewrite yui
 */
public interface OAuth2Service {

    /**
     * 处理OAuth2登录
     * 策略：
     * 1. 如果邮箱匹配到已有账号，直接登录
     * 2. 如果邮箱匹配到已有账号但需要验证，返回需要绑定状态
     * 3. 如果邮箱未匹配且OAuth2信息不足，返回需要补充信息状态
     *
     * @param oauth2UserInfo OAuth2用户信息
     * @return 登录响应（可能包含登录成功、需要绑定、需要补充信息等状态）
     */
    R<OAuth2LoginResponseDTO> handleOAuth2Login(OAuth2UserInfoDTO oauth2UserInfo);

    /**
     * 绑定已有账号
     * 将OAuth2账号绑定到已有的本地账号（通过邮箱和密码验证）
     *
     * @param bindBody 绑定请求体
     * @return 登录结果
     */
    R<OAuth2LoginResponseDTO> bindAccount(OAuth2BindAccountDTO bindBody);

    /**
     * 补充信息创建账号
     * 当OAuth2信息不足时，用户补充必要信息（邮箱、密码）后创建账号
     *
     * @param supplementBody 补充信息请求体
     * @return 登录结果
     */
    R<OAuth2LoginResponseDTO> supplementInfoAndCreateAccount(OAuth2SupplementInfoDTO supplementBody);

    /**
     * 解绑OAuth2账号
     * 解除当前用户与指定第三方平台的绑定关系
     *
     * @param userId 用户ID
     * @param provider 第三方提供商（github, orcid等）
     * @return 操作结果
     */
    R<Void> unbindAccount(Long userId, String provider);
}

