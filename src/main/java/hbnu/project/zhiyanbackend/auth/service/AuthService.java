package hbnu.project.zhiyanbackend.auth.service;

import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.basic.domain.R;

import java.util.Optional;

/**
 * 认证服务接口
 * 处理认证相关的核心逻辑
 *
 * @author ErgouTree
 */
public interface AuthService {


    /**
     * 用户注册
     *
     * @param request 注册请求体
     * @return 注册结果
     */
    R<UserRegisterResponseDTO> register(RegisterDTO request);

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求体
     * @return 登录结果
     */
    R<UserLoginResponseDTO> login(LoginDTO loginDTO);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    R<Boolean> checkEmail(String email);


    /**
     * 发送验证码
     *
     * @param verificationCodeDTO 验证码请求体
     * @return 发送结果
     */
    R<Void> sendVerificationCode(VerificationCodeDTO verificationCodeDTO);

    /**
     * 验证验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @param type  验证码类型
     * @return 验证结果
     */
    R<Boolean> verifyCode(String email, String code, String type);

    /**
     * 生成JWT令牌
     *
     * @param userId     用户ID
     * @param rememberMe 是否记住我
     * @return token信息
     */
    TokenDTO generateTokens(Long userId, boolean rememberMe);

    /**
     * 刷新JWT令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的token信息
     */
    TokenDTO refreshToken(String refreshToken);

    /**
     * 验证JWT令牌
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    String validateToken(String token);

    /**
     * 将token加入黑名单
     *
     * @param token  JWT令牌
     * @param userId 用户ID
     */
    void blacklistToken(String token, Long userId);

    /**
     * 检查token是否在黑名单中
     *
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    boolean isTokenBlacklisted(String token);

    /**
     * 验证令牌并返回详细验证结果
     */
    R<TokenValidateResponseDTO> validateTokenWithDetails(String token);

    /**
     * 重置密码
     *
     * @param request 重置密码请求
     * @return 操作结果
     */
    R<Void> resetPassword(ResetPasswordDTO request);

    /**
     * 忘记密码
     *
     * @param email 验证邮箱
     * @return 操作结果
     */
    R<Void> forgotPassword(String email);

    /**
     * 修改用户邮箱（改绑邮箱）
     * 验证新邮箱的验证码后，更新用户邮箱地址
     *
     * @param userId           用户ID（邮箱所属用户）
     * @param changeEmailBody  修改邮箱表单数据（包含新邮箱、验证码）
     * @return R<UserDTO> - 成功返回更新后的用户信息；失败返回错误信息（如验证码无效、邮箱已被使用等）
     */
    R<UserDTO> changeEmail(Long userId, ChangeEmailDTO changeEmailBody);

    /**
     * 用户登出
     *
     * @param tokenHeader 令牌头
     * @return 操作结果
     */
    R<Void> logout(String tokenHeader);

    /**
     * 创建RememberMe token
     *
     * @param userId 用户id
     * @return token
     */
    String createRememberMeToken(Long userId);

    /**
     * 验证RememberMe token
     *
     * @param token RememberToken
     * @return 验证结果
     */
    Optional<Long> validateRememberMeToken(String token);

    /**
     * 刷新RememberMe token过期时间
     * 在用户活跃时延长token的有效期
     *
     * @param userId 用户ID
     */
    void refreshRememberMeToken(Long userId);
}
