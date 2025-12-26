package hbnu.project.zhiyanbackend.auth.service.impl;

import cn.hutool.core.lang.UUID;
import hbnu.project.zhiyanbackend.auth.exeption.OAuth2Exception;
import hbnu.project.zhiyanbackend.auth.model.converter.UserConnectionMapper;
import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.model.entity.UserConnection;
import hbnu.project.zhiyanbackend.auth.model.enums.UserStatus;
import hbnu.project.zhiyanbackend.auth.oauth.provider.OrcidOAuth2Provider;
import hbnu.project.zhiyanbackend.auth.repository.UserConnectionRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.AuthService;
import hbnu.project.zhiyanbackend.auth.service.OAuth2Service;
import hbnu.project.zhiyanbackend.auth.service.RoleService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2第三方登录服务实现类
 * 重构后采用"主账号 + 绑定关系"模式
 *
 * @author ErgouTree
 * @modify yui
 * @rewrite ErgoyTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final UserRepository userRepository;
    private final UserConnectionRepository connectionRepository;
    private final AuthService authService;
    private final RoleService roleService;
    private final UserConverter userConverter;
    private final OrcidOAuth2Provider orcidOAuth2Provider;
    private final UserConnectionMapper userConnectionMapper;

    /**
     * 处理 OAuth2 登录/注册
     *
     * 流程：
     * 1. 查询 UserConnection，看第三方账号是否已绑定
     * 2. 已绑定 -> 直接登录
     * 3. 未绑定 -> 尝试邮箱匹配
     *    a. 匹配成功 -> 自动绑定 + 登录
     *    b. 匹配失败 -> 静默注册 + 自动绑定 + 登录
     */
    @Override
    public R<OAuth2LoginResponseDTO> handleOAuth2Login(OAuth2UserInfoDTO oauth2UserInfo) {
        log.info("处理OAuth2登录 - 提供商: {}, 用户ID: {}, 邮箱: {}", oauth2UserInfo.getProvider(), oauth2UserInfo.getProviderUserId(), oauth2UserInfo.getEmail());

        try {
            // 1. 验证OAuth2用户信息
            validateOAuth2UserInfo(oauth2UserInfo);

            String provider = oauth2UserInfo.getProvider();
            String providerUserId = oauth2UserInfo.getProviderUserId();

            // 2. 查询是否已有绑定关系
            Optional<UserConnection> connectionOpt = connectionRepository.findByProviderAndProviderUserIdAndIsUnboundFalse(provider, providerUserId);
            if(connectionOpt.isPresent()) {
                // 场景 1: 已绑定，直接登录
                UserConnection connection = connectionOpt.get();
                log.info("第三方账号已绑定 - 用户ID: {}, 提供商: {}", connection.getUserId(), provider);

                // 更新绑定信息（头像、用户名等可能变化）
                updateConnectionInfo(connection, oauth2UserInfo);
                connectionRepository.save(connection);

                // 执行登录
                return doLogin(connection.getUserId(), oauth2UserInfo);
            }

            // 3. 未绑定，尝试邮箱匹配
            if (StringUtils.isNotBlank(oauth2UserInfo.getEmail())) {
                Optional<User> userByEmailOpt = userRepository.findByEmailAndIsDeletedFalse(oauth2UserInfo.getEmail());
                if (userByEmailOpt.isPresent()) {
                    // 场景 2: 邮箱匹配成功，自动绑定
                    User existingUser = userByEmailOpt.get();
                    log.info("邮箱匹配成功，自动绑定 - 用户ID: {}, 邮箱: {}, 提供商: {}", existingUser.getId(), existingUser.getEmail(), provider);
                    // 检查用户状态
                    if (Boolean.TRUE.equals(existingUser.getIsLocked())) {
                        return R.fail("账户已被锁定，请联系管理员");
                    }
                    // 创建绑定关系
                    UserConnection newConnection = createConnection(existingUser.getId(), oauth2UserInfo);
                    connectionRepository.save(newConnection);

                    // 更新用户信息（头像等）
                    updateUserFromOAuth2(existingUser, oauth2UserInfo);
                    userRepository.save(existingUser);

                    // 执行登录
                    return doLogin(existingUser.getId(), oauth2UserInfo);
                }
            }
            // 场景 3: 邮箱不匹配或没有邮箱，静默注册新用户
            log.info("静默注册新用户 - 提供商: {}, 提供商用户ID: {}", provider, providerUserId);
            User newUser = createUserFromOAuth2(oauth2UserInfo);
            User savedUser = userRepository.save(newUser);

            // 分配默认角色
            assignDefaultRole(savedUser.getId());

            // 创建绑定关系
            UserConnection newConnection = createConnection(savedUser.getId(), oauth2UserInfo);
            connectionRepository.save(newConnection);

            // 执行登录
            return doLogin(savedUser.getId(), oauth2UserInfo);
        }catch (ServiceException e){
            log.error("OAuth2 登录失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("OAuth2 登录异常", e);
            return R.fail("登录失败，请稍后重试");
        }
    }


    /**
     * 已登录用户手动绑定第三方账号
     */
    @Override
    @Transactional
    public R<Void> bindOAuth2Account(Long userId, OAuth2UserInfoDTO oauth2UserInfo) {
        log.info("手动绑定第三方账号 - 用户ID: {}, 提供商: {}", userId, oauth2UserInfo.getProvider());

        try{
            // 1. 验证用户是否存在
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }

            User user = userOpt.get();
            if (Boolean.TRUE.equals(user.getIsLocked())) {
                return R.fail("账户已被锁定");
            }

            String provider = oauth2UserInfo.getProvider();
            String providerUserId = oauth2UserInfo.getProviderUserId();

            // 2. 检查该第三方账号是否已被其他用户绑定
            Optional<UserConnection>existingConnectionOpt = connectionRepository.findByProviderAndProviderUserIdAndIsUnboundFalse(provider, providerUserId);
            if (existingConnectionOpt.isPresent()) {
                UserConnection existingConnection = existingConnectionOpt.get();
                if (!existingConnection.getUserId().equals(userId)) {
                    return R.fail("该 " + provider + " 账号已被其他用户绑定，请先解绑");
                }
                // 已绑定到当前用户，无需重复绑定
                log.info("该账号已绑定到当前用户 - 用户ID: {}, 提供商: {}", userId, provider);
                return R.ok(null, "该账号已绑定");
            }

            // 3.检查当前用户是否已绑定过该提供商
            boolean alreadyBound = connectionRepository.existsByUserIdAndProviderAndIsUnboundFalse(userId, provider);
            if (alreadyBound) {
                return R.fail("您已绑定过 " + provider + " 账号，请先解绑旧账号");
            }

            // 4. 创建绑定关系
            UserConnection newConnection = createConnection(userId, oauth2UserInfo);
            connectionRepository.save(newConnection);

            // 5.更新用户信息
            updateUserFromOAuth2(user, oauth2UserInfo);
            userRepository.save(user);

            log.info("手动绑定成功 - 用户ID: {}, 提供商: {}, 提供商用户ID: {}", userId, provider, providerUserId);
            return R.ok(null, "绑定成功");
        }catch (ServiceException e){
            log.error("手动绑定失败 - 用户ID: {}, 提供商: {}", userId, oauth2UserInfo.getProvider(), e);
            return R.fail("绑定失败，请稍后重试");
        }
    }

    /**
     * 解绑第三方账号
     */
    @Override
    @Transactional
    public R<Void> unbindOAuth2Account(Long userId, String provider) {
        log.info("解绑第三方账号 - 用户ID: {}, 提供商: {}", userId, provider);

        try{
            // 1.查询用户
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }
            User user = userOpt.get();

            // 2.查询绑定关系
            Optional<UserConnection> connectionOpt = connectionRepository.findByUserIdAndProviderAndIsUnboundFalse(userId, provider);
            if (connectionOpt.isEmpty()) {
                return R.fail("未绑定该 " + provider + " 账号");
            }

            // 3.安全检查，如果用户没有密码或者邮箱，且只剩下一个绑定，就不允许解绑
            if (!user.hasPassword() | !user.hasEmail()) {
                long connectionCount = connectionRepository.countByUserId(userId);
                if (connectionCount <= 1) {
                    return R.fail("因为您尚未设置密码或者邮箱，且这是唯一的登录方式，解绑后将无法登录。请先设置密码后再解绑。");
                }
            }

            // 4. 标记为解绑（软删除）
            UserConnection connection = connectionOpt.get();
            connection.setIsUnbound(true);
            connectionRepository.save(connection);

            log.info("解绑成功 - 用户ID: {}, 提供商: {}", userId, provider);
            return R.ok(null, "解绑成功");
        } catch (Exception e) {
            log.error("解绑失败 - 用户ID: {}, 提供商: {}", userId, provider, e);
            return R.fail("解绑失败，请稍后重试");
        }
    }


    /**
     * 查询用户的所有绑定关系
     */
    @Override
    public R<List<UserConnectionDTO>> getUserConnections(Long userId) {
        try {
            List<UserConnection> connections = connectionRepository.findByUserIdAndIsUnboundFalse(userId);
            List<UserConnectionDTO> dtos = connections.stream()
                    .map(userConnectionMapper::toDTO)
                    .collect(Collectors.toList());
            return R.ok(dtos);
        } catch (Exception e) {
            log.error("查询用户绑定关系失败 - 用户ID: {}", userId, e);
            return R.fail("查询失败");
        }
    }


    /**
     * 检查第三方账号是否已被绑定
     */
    @Override
    public boolean isOAuth2AccountBound(String provider, String providerUserId) {
        return connectionRepository.existsByProviderAndProviderUserIdAndIsUnboundFalse(
                provider, providerUserId);
    }


    /**
     * 获取ORCID用户详细信息
     */
    @Override
    public R<OrcidDetailDTO> getOrcidDetail(Long userId) {
        try{
            log.info("获取ORCID详细信息 - 用户ID: {}", userId);

            // 1. 查询用户是否存在
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }

            // 2. 检查用户是否绑定ORCID
            Optional<UserConnection> connectionOpt = connectionRepository.findByUserIdAndProviderAndIsUnboundFalse(userId, "orcid");
            if (connectionOpt.isEmpty()) {
                return R.fail("用户未绑定ORCID账号");
            }

            UserConnection orcidConnection = connectionOpt.get();
            String orcidId = orcidConnection.getProviderUserId();

            if (StringUtils.isBlank(orcidId)) {
                return R.fail("未获取到ORCID ID");
            }

            // 3. 检查ORCID Provider是否启用
            if (!orcidOAuth2Provider.isEnabled()) {
                return R.fail("后端配置未启用ORCID功能");
            }

            // 4. 获取AccessToken
            String accessToken = orcidConnection.getAccessToken();
            if (StringUtils.isBlank(accessToken)) {
                return R.fail("未获取到有效的ORCID授权Token，请尝试重新绑定账号");
            }

            // 5. 使用Provider获取详细信息
            OrcidDetailDTO detail = orcidOAuth2Provider.getOrcidDetailInfo(orcidId, accessToken);

            log.info("成功获取ORCID详细信息 - 用户ID: {}, ORCID: {}", userId, orcidId);
            return R.ok(detail);
        }catch (ServiceException e){
            log.error("获取ORCID详细信息失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取ORCID详细信息异常", e);
            return R.fail("获取失败，请稍后重试");
        }
    }

    // ==================== 私有辅助方法 ====================


    /**
     * 执行登录逻辑
     */
    private R<OAuth2LoginResponseDTO> doLogin(Long userId, OAuth2UserInfoDTO oauth2UserInfo) {
        try{
            // 查询用户
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }
            User user = userOpt.get();

            // 检查用户状态
            if(Boolean.TRUE.equals(user.getIsLocked())) {
                return R.fail("账户已被锁定，请联系管理员");
            }

            // 生成 JWT Token
            boolean rememberMe = false;
            TokenDTO tokenDTO = authService.generateTokens(userId, rememberMe);

            // 获取用户角色
            R<Set<String>> rolesResult = roleService.getUserRoles(userId);
            List<String> roleNames = rolesResult.getData() != null
                    ? new ArrayList<>(rolesResult.getData())
                    : new ArrayList<>();

            UserDTO userDTO = userConverter.toDTO(user);
            userDTO.setRoles(roleNames);

            // 构建登录响应
            UserLoginResponseDTO loginResponse = UserLoginResponseDTO.builder()
                    .user(userDTO)
                    .accessToken(tokenDTO.getAccessToken())
                    .refreshToken(tokenDTO.getRefreshToken())
                    .expiresIn(tokenDTO.getExpiresIn())
                    .tokenType(tokenDTO.getTokenType())
                    .rememberMe(rememberMe)
                    .build();

            log.info("OAuth2 登录成功 - 用户ID: {}, 邮箱: {}, 提供商: {}", userId, user.getEmail(), oauth2UserInfo.getProvider());

            return R.ok(OAuth2LoginResponseDTO.success(loginResponse));
        }catch (ServiceException e){
            log.error("执行登录失败 - 用户ID: {}", userId, e);
            return R.fail("登录失败，请稍后重试");
        }
    }

    /**
     * 从 OAuth2 信息创建新用户
     */
    private User createUserFromOAuth2(OAuth2UserInfoDTO oauth2UserInfo) {
        // 生成用户名
        String name = generateUsername(oauth2UserInfo);

        User newUser = User.builder()
                // 可能为 null
                .email(oauth2UserInfo.getEmail())
                // 纯 OAuth2 用户没有密码
                .passwordHash(null)
                .name(name)
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .isLocked(false)
                .build();

        log.info("创建新用户 - 用户名: {}, 邮箱: {}, 提供商: {}", name, oauth2UserInfo.getEmail(), oauth2UserInfo.getProvider());

        return newUser;
    }

    /**
     * 生成用户名
     * 优先级：OAuth2 昵称 > OAuth2 用户名 > 邮箱前缀 > 随机字符串
     */
    private String generateUsername(OAuth2UserInfoDTO oauth2UserInfo) {
        if (StringUtils.isNotBlank(oauth2UserInfo.getNickname())) {
            return oauth2UserInfo.getNickname();
        }
        if (StringUtils.isNotBlank(oauth2UserInfo.getUsername())) {
            return oauth2UserInfo.getUsername();
        }
        if (StringUtils.isNotBlank(oauth2UserInfo.getEmail())) {
            String emailPrefix = oauth2UserInfo.getEmail().split("@")[0];
            if (StringUtils.isNotBlank(emailPrefix)) {
                return emailPrefix;
            }
        }
        // 生成随机用户名：user_随机8位字符
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 创建绑定关系
     */
    private UserConnection createConnection(Long userId, OAuth2UserInfoDTO oauth2UserInfo) {
        return UserConnection.builder()
                .userId(userId)
                .provider(oauth2UserInfo.getProvider())
                .providerUserId(oauth2UserInfo.getProviderUserId())
                .providerUsername(oauth2UserInfo.getUsername())
                .providerEmail(oauth2UserInfo.getEmail())
                .accessToken(oauth2UserInfo.getAccessToken())
                .isUnbound(false)
                .lastSyncAt(System.currentTimeMillis())
                .build();
    }

    /**
     * 更新绑定信息（头像、用户名等可能变化）
     */
    private void updateConnectionInfo(UserConnection connection, OAuth2UserInfoDTO oauth2UserInfo) {
        connection.setProviderUsername(oauth2UserInfo.getUsername());
        connection.setProviderEmail(oauth2UserInfo.getEmail());
        connection.setAccessToken(oauth2UserInfo.getAccessToken());
        connection.setLastSyncAt(System.currentTimeMillis());
    }

    /**
     * 更新用户信息（从 OAuth2）
     * 注意：仅在用户同意的情况下更新，避免覆盖用户手动修改的信息
     */
    private void updateUserFromOAuth2(User user, OAuth2UserInfoDTO oauth2UserInfo) {
        // 如果用户没有邮箱，且 OAuth2 提供了邮箱，则更新
        if (!user.hasEmail() && StringUtils.isNotBlank(oauth2UserInfo.getEmail())) {
            user.setEmail(oauth2UserInfo.getEmail());
            log.info("更新用户邮箱 - 用户ID: {}, 新邮箱: {}", user.getId(), oauth2UserInfo.getEmail());
        }
        // 头像使用默认的，不适用第三方平台的
    }

    /**
     * 分配默认角色
     */
    private void assignDefaultRole(Long userId) {
        try {
            log.info("为新用户分配默认角色 - 用户ID: {}", userId);
            R<Long> roleResult = roleService.getRoleIdByName("USER");

            if (R.isSuccess(roleResult) && roleResult.getData() != null) {
                Long roleId = roleResult.getData();
                R<Void> assignResult = roleService.assignRolesToUser(userId, List.of(roleId));

                if (R.isSuccess(assignResult)) {
                    log.info("成功为用户分配默认角色 USER - 用户ID: {}", userId);
                } else {
                    log.warn("为用户分配默认角色失败 - 用户ID: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("分配默认角色异常 - 用户ID: {}", userId, e);
        }
    }

    /**
     * 验证 OAuth2 用户信息
     */
    private void validateOAuth2UserInfo(OAuth2UserInfoDTO oauth2UserInfo) {
        if (oauth2UserInfo == null) {
            throw new OAuth2Exception("OAuth2 用户信息不能为空");
        }
        if (StringUtils.isBlank(oauth2UserInfo.getProvider())) {
            throw new OAuth2Exception("OAuth2 提供商不能为空");
        }
        if (StringUtils.isBlank(oauth2UserInfo.getProviderUserId())) {
            throw new OAuth2Exception("OAuth2 用户ID不能为空");
        }
    }
}