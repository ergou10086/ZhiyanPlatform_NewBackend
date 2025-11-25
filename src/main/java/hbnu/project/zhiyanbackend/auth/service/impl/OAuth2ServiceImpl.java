package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2BindAccountDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2LoginResponseDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2SupplementInfoDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.OAuth2UserInfoDTO;
import hbnu.project.zhiyanbackend.auth.service.OAuth2Service;
import hbnu.project.zhiyanbackend.basic.domain.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    /**
     * 处理OAuth2登录
     *
     * @param oauth2UserInfo OAuth2用户信息
     * @return 登录响应（可能包含登录成功、需要绑定、需要补充信息等状态）
     */
    @Override
    public R<OAuth2LoginResponseDTO> handleOAuth2Login(OAuth2UserInfoDTO oauth2UserInfo) {
        return null;
    }

    /**
     * 绑定已有账号
     * 将OAuth2账号绑定到已有的本地账号（通过邮箱和密码验证）
     *
     * @param bindBody 绑定请求体
     * @return 登录结果
     */
    @Override
    public R<OAuth2LoginResponseDTO> bindAccount(OAuth2BindAccountDTO bindBody) {
        return null;
    }

    /**
     * 补充信息创建账号
     * 当OAuth2信息不足时，用户补充必要信息（邮箱、密码）后创建账号
     *
     * @param supplementBody 补充信息请求体
     * @return 登录结果
     */
    @Override
    public R<OAuth2LoginResponseDTO> supplementInfoAndCreateAccount(OAuth2SupplementInfoDTO supplementBody) {
        return null;
    }
}
