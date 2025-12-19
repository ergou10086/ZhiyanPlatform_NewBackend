package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.exeption.OAuth2Exception;
import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.model.enums.UserStatus;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.AuthService;
import hbnu.project.zhiyanbackend.auth.service.OAuth2Service;
import hbnu.project.zhiyanbackend.auth.service.RoleService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.PasswordUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * OAuth2第三方登录服务实现类
 *
 * @author ErgouTree
 * @rewrite yui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;

    /**
     * 处理OAuth2登录
     * 策略：
     * 1. 如果邮箱匹配到已有账号，直接登录
     * 2. 如果邮箱未匹配，返回需要绑定或补充信息的状态
     */
    @Override
    public R<OAuth2LoginResponseDTO> handleOAuth2Login(OAuth2UserInfoDTO oauth2UserInfo) {
        log.info("处理OAuth2登录 - 提供商: {}, 用户ID: {}, 邮箱: {}",
                oauth2UserInfo.getProvider(), oauth2UserInfo.getProviderUserId(), oauth2UserInfo.getEmail());

        try {
            // 1. 验证OAuth2用户信息
            validateOAuth2UserInfo(oauth2UserInfo);

            // 2. 检查邮箱是否匹配到已有账号
            if (StringUtils.isNotBlank(oauth2UserInfo.getEmail())) {
                Optional<User> userOpt = userRepository.findByEmailAndIsDeletedFalse(oauth2UserInfo.getEmail());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // 检查用户状态
                    if (Boolean.TRUE.equals(user.getIsLocked())) {
                        return R.fail("账户已被锁定，请联系管理员");
                    }

                    // 邮箱匹配到账号，直接登录
                    log.info("邮箱匹配到已有账号，直接登录 - 用户ID: {}, 邮箱: {}", user.getId(), user.getEmail());
                    return doLogin(user, oauth2UserInfo);
                } else {
                    // 邮箱未匹配到账号，引导用户绑定已有账号或创建新账号
                    log.info("邮箱未匹配到账号，需要绑定或创建 - 邮箱: {}", oauth2UserInfo.getEmail());
                    return R.ok(OAuth2LoginResponseDTO.needBind(oauth2UserInfo, oauth2UserInfo.getEmail()));
                }
            } else {
                // OAuth2未提供邮箱，必须补充信息
                log.info("OAuth2未提供邮箱，需要补充信息 - 提供商: {}, 用户ID: {}",
                        oauth2UserInfo.getProvider(), oauth2UserInfo.getProviderUserId());
                return R.ok(OAuth2LoginResponseDTO.needSupplement(oauth2UserInfo));
            }

        } catch (OAuth2Exception e) {
            log.error("OAuth2登录失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("OAuth2登录异常", e);
            return R.fail("登录失败，请稍后重试");
        }
    }

    /**
     * 执行登录逻辑（生成Token等）
     */
    private R<OAuth2LoginResponseDTO> doLogin(User user, OAuth2UserInfoDTO oauth2UserInfo) {
        // 更新用户信息（头像等可能变化）
        updateUserFromOAuth2(user, oauth2UserInfo);
        userRepository.save(user);

        // 生成JWT Token
        boolean rememberMe = false;
        TokenDTO tokenDTO = authService.generateTokens(user.getId(), rememberMe);

        // 获取用户角色信息
        R<Set<String>> rolesResult = roleService.getUserRoles(user.getId());
        List<String> roleNames = rolesResult.getData() != null
                ? new ArrayList<>(rolesResult.getData())
                : new ArrayList<>();

        // 转换为UserDTO
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
                .rememberMeToken(null)
                .build();

        log.info("OAuth2登录成功 - 用户ID: {}, 邮箱: {}, 提供商: {}",
                user.getId(), user.getEmail(), oauth2UserInfo.getProvider());

        return R.ok(OAuth2LoginResponseDTO.success(loginResponse));
    }

    /**
     * 验证OAuth2用户信息
     */
    private void validateOAuth2UserInfo(OAuth2UserInfoDTO oauth2UserInfo) {
        if (oauth2UserInfo == null) {
            throw new OAuth2Exception("OAuth2用户信息不能为空");
        }

        if (StringUtils.isBlank(oauth2UserInfo.getProvider())) {
            throw new OAuth2Exception("OAuth2提供商不能为空");
        }

        if (StringUtils.isBlank(oauth2UserInfo.getProviderUserId())) {
            throw new OAuth2Exception("OAuth2用户ID不能为空");
        }

        // 邮箱可以为空（某些OAuth2提供商可能不提供邮箱）
        // 但如果邮箱为空，我们需要使用其他方式标识用户
        if (StringUtils.isBlank(oauth2UserInfo.getEmail()) && StringUtils.isBlank(oauth2UserInfo.getUsername())) {
            throw new OAuth2Exception("OAuth2用户信息不完整：缺少邮箱或用户名");
        }
    }

    /**
     * 绑定已有账号
     * 验证邮箱和密码后，将OAuth2账号绑定到已有账号
     * 注意：绑定的邮箱必须与OAuth2提供的邮箱一致（安全考虑）
     */
    @Override
    @Transactional
    public R<OAuth2LoginResponseDTO> bindAccount(OAuth2BindAccountDTO bindBody) {
        log.info("绑定OAuth2账号到已有账号 - 提供商: {}, 邮箱: {}",
                bindBody.getProvider(), bindBody.getEmail());

        try {
            // 1. 验证OAuth2信息
            OAuth2UserInfoDTO oauth2UserInfo = bindBody.getOauth2UserInfo();
            if (oauth2UserInfo == null) {
                return R.fail("OAuth2用户信息不能为空");
            }

            // 2. 验证邮箱是否与OAuth2邮箱一致（安全考虑：只能绑定OAuth2提供的邮箱对应的账号）
            if (StringUtils.isNotBlank(oauth2UserInfo.getEmail())
                    && !oauth2UserInfo.getEmail().equals(bindBody.getEmail())) {
                return R.fail("绑定的邮箱必须与OAuth2账号的邮箱一致");
            }

            // 3. 查找用户
            Optional<User> userOpt = userRepository.findByEmailAndIsDeletedFalse(bindBody.getEmail());
            if (userOpt.isEmpty()) {
                return R.fail("邮箱未注册，无法绑定。请使用补充信息功能创建新账号");
            }

            User user = userOpt.get();

            // 4. 检查用户状态
            if (Boolean.TRUE.equals(user.getIsLocked())) {
                return R.fail("账户已被锁定，请联系管理员");
            }

            // 5. 验证密码
            if (!passwordEncoder.matches(bindBody.getPassword(), user.getPasswordHash())) {
                return R.fail("密码错误，绑定失败");
            }

            // 6. 验证OAuth2用户ID是否匹配（确保是同一个OAuth2账号）
            if (!bindBody.getProviderUserId().equals(oauth2UserInfo.getProviderUserId())) {
                return R.fail("OAuth2用户信息不匹配");
            }

            // 7. 更新用户信息（头像等）
            updateUserFromOAuth2(user, oauth2UserInfo);
            userRepository.save(user);

            // 8. 执行登录
            log.info("OAuth2账号绑定成功 - 用户ID: {}, 邮箱: {}, 提供商: {}",
                    user.getId(), user.getEmail(), bindBody.getProvider());

            return doLogin(user, oauth2UserInfo);

        } catch (Exception e) {
            log.error("绑定OAuth2账号失败", e);
            return R.fail("绑定失败，请稍后重试");
        }
    }

    /**
     * 补充信息创建账号
     * 用户补充邮箱和密码后创建新账号
     */
    @Override
    @Transactional
    public R<OAuth2LoginResponseDTO> supplementInfoAndCreateAccount(OAuth2SupplementInfoDTO supplementBody) {
        log.info("补充信息创建账号 - 提供商: {}, 邮箱: {}",
                supplementBody.getProvider(), supplementBody.getEmail());

        try {
            // 1. 验证密码一致性
            if (!supplementBody.getPassword().equals(supplementBody.getConfirmPassword())) {
                return R.fail("两次输入的密码不一致");
            }

            // 2. 验证密码强度
            if (!PasswordUtils.isValidPassword(supplementBody.getPassword())) {
                return R.fail("密码必须为7-25位，且必须包含至少一个字母");
            }
            // 检查密码是否与邮箱相同
            if (supplementBody.getPassword().equalsIgnoreCase(supplementBody.getEmail())) {
                return R.fail("密码不能与邮箱相同");
            }

            // 3. 验证OAuth2信息
            OAuth2UserInfoDTO oauth2UserInfo = supplementBody.getOauth2UserInfo();
            if (oauth2UserInfo == null) {
                return R.fail("OAuth2用户信息不能为空");
            }

            // 4. 验证OAuth2用户ID是否匹配
            if (!supplementBody.getProviderUserId().equals(oauth2UserInfo.getProviderUserId())) {
                return R.fail("OAuth2用户信息不匹配");
            }

            // 5. 检查邮箱是否已注册
            if (userRepository.existsByEmail(supplementBody.getEmail())) {
                return R.fail("该邮箱已被注册，请使用绑定账号功能");
            }

            // 6. 创建新用户
            // 确定用户名：优先使用OAuth2昵称，其次使用用户名，最后使用邮箱前缀
            String name;
            if (StringUtils.isNotBlank(oauth2UserInfo.getNickname())) {
                name = oauth2UserInfo.getNickname();
            } else if (StringUtils.isNotBlank(oauth2UserInfo.getUsername())) {
                name = oauth2UserInfo.getUsername();
            } else {
                String emailPrefix = supplementBody.getEmail().split("@")[0];
                name = StringUtils.isNotBlank(emailPrefix) ? emailPrefix : "用户";
            }

            User newUser = User.builder()
                    .id(hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils.nextId())
                    .email(supplementBody.getEmail())
                    .passwordHash(passwordEncoder.encode(supplementBody.getPassword()))
                    .name(name)
                    // OAuth2头像URL需要下载后存储，这里先设为null
                    .avatarData(null)
                    .avatarContentType(null)
                    .avatarSize(null)
                    .title(null)
                    .institution(null)
                    .status(UserStatus.ACTIVE)
                    .isDeleted(false)
                    .isLocked(false)
                    .researchTags("")
                    .build();

            // 绑定OAuth2账号（在保存前设置）
            String provider = supplementBody.getProvider();
            if (StringUtils.isNotBlank(provider) && StringUtils.isNotBlank(oauth2UserInfo.getProviderUserId())) {
                if ("github".equalsIgnoreCase(provider)) {
                    newUser.setGithubId(oauth2UserInfo.getProviderUserId());
                    if (StringUtils.isNotBlank(oauth2UserInfo.getUsername())) {
                        newUser.setGithubUsername(oauth2UserInfo.getUsername());
                    }
                } else if ("orcid".equalsIgnoreCase(provider)) {
                    newUser.setOrcidId(oauth2UserInfo.getProviderUserId());
                    newUser.setOrcidBound(true);
                }
            }

            User savedUser = userRepository.save(newUser);

            // 7. 分配默认角色
            assignDefaultRoleToUser(savedUser.getId());

            log.info("新用户创建成功 - 用户ID: {}, 邮箱: {}", savedUser.getId(), savedUser.getEmail());

            // 8. 更新OAuth2用户信息的邮箱（确保一致性）
            oauth2UserInfo.setEmail(supplementBody.getEmail());

            // 9. 执行登录
            return doLogin(savedUser, oauth2UserInfo);

        } catch (Exception e) {
            log.error("补充信息创建账号失败", e);
            return R.fail("创建账号失败，请稍后重试");
        }
    }

    /**
     * 更新用户信息（从OAuth2获取的最新信息）
     * 注意：新架构使用avatarData（BYTEA）存储头像，OAuth2提供的是URL，需要下载后存储
     * 这里暂时只更新其他信息，头像先不更新
     * 同时绑定OAuth2账号（GitHub、ORCID等）
     */
    private void updateUserFromOAuth2(User user, OAuth2UserInfoDTO oauth2UserInfo) {
        // 更新昵称（如果OAuth2提供了昵称且与当前不同）
        // 注意：不自动更新，避免覆盖用户手动修改的名称
        // if (StringUtils.isNotBlank(oauth2UserInfo.getNickname())
        //         && !oauth2UserInfo.getNickname().equals(user.getName())) {
        //     user.setName(oauth2UserInfo.getNickname());
        // }

        // 如果用户没有邮箱，且OAuth2提供了邮箱，则更新
        if (StringUtils.isBlank(user.getEmail()) && StringUtils.isNotBlank(oauth2UserInfo.getEmail())) {
            user.setEmail(oauth2UserInfo.getEmail());
        }

        // 绑定OAuth2账号
        String provider = oauth2UserInfo.getProvider();
        if (StringUtils.isNotBlank(provider) && StringUtils.isNotBlank(oauth2UserInfo.getProviderUserId())) {
            if ("github".equalsIgnoreCase(provider)) {
                // 绑定GitHub账号
                user.setGithubId(oauth2UserInfo.getProviderUserId());
                if (StringUtils.isNotBlank(oauth2UserInfo.getUsername())) {
                    user.setGithubUsername(oauth2UserInfo.getUsername());
                }
                log.info("绑定GitHub账号 - 用户ID: {}, GitHub ID: {}", user.getId(), oauth2UserInfo.getProviderUserId());
            } else if ("orcid".equalsIgnoreCase(provider)) {
                // 绑定ORCID账号
                user.setOrcidId(oauth2UserInfo.getProviderUserId());
                user.setOrcidBound(true);
                log.info("绑定ORCID账号 - 用户ID: {}, ORCID ID: {}", user.getId(), oauth2UserInfo.getProviderUserId());

                // 保存ORCID访问令牌，用于后续获取详细信息
                if (StringUtils.isNotBlank(oauth2UserInfo.getAccessToken())) {
                    user.setOrcidAccessToken(oauth2UserInfo.getAccessToken());
                }
            }
        }
    }

    /**
     * 为新用户分配默认角色
     */
    private void assignDefaultRoleToUser(Long userId) {
        try {
            log.info("为新用户分配默认角色 - 用户ID: {}", userId);
            R<Long> roleResult = roleService.getRoleIdByName("USER");

            if (R.isSuccess(roleResult) && roleResult.getData() != null) {
                Long roleId = roleResult.getData();
                R<Void> assignResult = roleService.assignRolesToUser(userId, List.of(roleId));

                if (R.isSuccess(assignResult)) {
                    log.info("成功为用户分配默认角色 USER - 用户ID: {}", userId);
                } else {
                    log.warn("为用户分配默认角色失败 - 用户ID: {}, 错误: {}", userId, assignResult.getMsg());
                }
            } else {
                log.warn("未找到 USER 角色，无法分配默认角色 - 用户ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("为用户分配默认角色发生异常 - 用户ID: {}", userId, e);
        }
    }

    /**
     * 解绑OAuth2账号
     * 解除当前用户与指定第三方平台的绑定关系
     */
    @Override
    @Transactional
    public R<Void> unbindAccount(Long userId, String provider) {
        log.info("解绑OAuth2账号 - 用户ID: {}, 提供商: {}", userId, provider);

        try {
            // 1. 查找用户
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }

            User user = userOpt.get();

            // 2. 根据提供商解绑
            if ("github".equalsIgnoreCase(provider)) {
                if (StringUtils.isBlank(user.getGithubId())) {
                    return R.fail("未绑定GitHub账号，无需解绑");
                }
                user.setGithubId(null);
                user.setGithubUsername(null);
                log.info("解绑GitHub账号成功 - 用户ID: {}", userId);
            } else if ("orcid".equalsIgnoreCase(provider)) {
                if (StringUtils.isBlank(user.getOrcidId())) {
                    return R.fail("未绑定ORCID账号，无需解绑");
                }
                user.setOrcidId(null);
                user.setOrcidBound(false);
                log.info("解绑ORCID账号成功 - 用户ID: {}", userId);
            } else {
                return R.fail("不支持的第三方平台: " + provider);
            }

            // 3. 保存用户信息
            userRepository.save(user);

            return R.ok(null, "解绑成功");
        } catch (Exception e) {
            log.error("解绑OAuth2账号失败 - 用户ID: {}, 提供商: {}", userId, provider, e);
            return R.fail("解绑失败，请稍后重试");
        }
    }
}