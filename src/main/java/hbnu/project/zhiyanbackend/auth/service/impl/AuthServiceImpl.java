package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.model.entity.RememberMeToken;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.model.enums.UserStatus;
import hbnu.project.zhiyanbackend.auth.model.enums.VerificationCodeType;
import hbnu.project.zhiyanbackend.auth.repository.RememberMeTokenRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.AuthService;
import hbnu.project.zhiyanbackend.auth.service.RoleService;
import hbnu.project.zhiyanbackend.auth.service.VerificationCodeService;
import hbnu.project.zhiyanbackend.basic.constants.CacheConstants;
import hbnu.project.zhiyanbackend.basic.constants.TokenConstants;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.JwtUtils;
import hbnu.project.zhiyanbackend.redis.service.RedisService;
import hbnu.project.zhiyanbackend.security.context.LoginUserBody;
import hbnu.project.zhiyanbackend.security.utils.PasswordUtils;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * 处理用户注册、登录、JWT令牌管理、RememberMe功能等认证相关核心逻辑
 * 
 * 实现说明：
 * 1. 用户注册：邮箱验证、密码校验、默认角色分配
 * 2. 用户登录：Spring Security标准认证流程、JWT令牌生成、RememberMe支持
 * 3. JWT令牌管理：生成、刷新、验证、黑名单管理
 * 4. RememberMe功能：持久化登录token管理
 * 5. 密码管理：重置密码、忘记密码
 * 6. 邮箱管理：修改邮箱、验证码验证
 * 
 * 优化说明：
 * - 新架构中RememberMe功能集成到AuthService，不再单独的服务类
 * - 保持与原架构相同的业务逻辑，确保功能一致性
 * - 完善的异常处理和日志记录
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final VerificationCodeService verificationCodeService;
    private final UserRepository userRepository;
    private final RememberMeTokenRepository rememberMeTokenRepository;
    private final RedisService redisService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final AuthenticationManager authenticationManager;
    private final AuthUserDetailsServiceImpl authUserDetailsService;
    private final UserConverter userConverter;

    // RememberMe token有效期：30天
    private static final int REMEMBER_ME_DAYS = 30;


    /**
     * 用户注册
     * 完成邮箱验证、密码校验、用户创建、默认角色分配和自动登录
     * 
     * 优化说明：
     * - 注册后自动登录，提升用户体验
     * - 自动分配USER角色，确保新用户有基本权限
     * - 密码强度校验，提升安全性
     *
     * @param request 注册请求体
     * @return 注册结果（包含用户信息和JWT令牌）
     */
    @Override
    @Transactional
    public R<UserRegisterResponseDTO> register(RegisterDTO request) {
        log.info("处理用户注册: 邮箱={}, 姓名={}", request.getEmail(), request.getName());

        try {
            // 1. 邮箱唯一性校验，一个邮箱只能注册一个用户
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return R.fail("该邮箱已被注册");
            }

            // 2. 密码一致性校验
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return R.fail("两次输入的密码不一致");
            }

            // 3. 密码合法性校验和强度校验
            if (!PasswordUtils.isValidPassword(request.getPassword())) {
                return R.fail("密码必须为6-16位");
            }
            validatePasswordStrength(request.getPassword());

            // 4. 校验验证码
            R<Boolean> validateResult = verificationCodeService.validateCode(
                    request.getEmail(), request.getVerificationCode(), VerificationCodeType.REGISTER
            );
            if (!R.isSuccess(validateResult) || Boolean.FALSE.equals(validateResult.getData())) {
                return R.fail("验证码错误或已过期");
            }

            // 5. 创建用户
            User user = User.builder()
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .title(request.getTitle())
                    .institution(request.getInstitution())
                    .status(UserStatus.ACTIVE)
                    .isDeleted(false)
                    .isLocked(false)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("用户创建成功，开始分配角色 - 用户ID: {}", savedUser.getId());

            // 6. 为新用户分配默认角色（普通用户/成员）
            assignDefaultRoleToUser(savedUser.getId());
            log.info("角色分配流程完成 - 用户ID: {}", savedUser.getId());

            // 7. 生成JWT令牌，注册后自动登录
            boolean rememberMe = false; // 注册时固定为 false
            TokenDTO tokenDTO = generateTokens(savedUser.getId(), rememberMe);

            // 8. 计算密码强度描述
            int passwordStrength = PasswordUtils.validatePasswordStrength(request.getPassword());
            String passwordStrengthDesc = getPasswordStrengthDescription(passwordStrength);

            // 9. 构建完整的响应（包含登录令牌）
            UserRegisterResponseDTO response = UserRegisterResponseDTO.builder()
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .name(savedUser.getName())
                    .title(savedUser.getTitle())
                    .institution(savedUser.getInstitution())
                    .accessToken(tokenDTO.getAccessToken())
                    .refreshToken(tokenDTO.getRefreshToken())
                    .expiresIn(tokenDTO.getExpiresIn())
                    .tokenType(tokenDTO.getTokenType())
                    .passwordStrength(passwordStrengthDesc)
                    .rememberMe(rememberMe)
                    .build();

            log.info("用户注册成功并自动登录: 用户ID={}, 邮箱={}", savedUser.getId(), savedUser.getEmail());
            return R.ok(response, "注册成功");

        } catch (Exception e) {
            log.error("用户注册失败 - 邮箱: {}, 错误: {}", request.getEmail(), e.getMessage(), e);
            return R.fail("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录（使用 Spring Security 标准认证流程）
     * 会自动调用 AuthUserDetailsService.loadUserByUsername() 进行用户信息加载和密码验证
     * 
     * 优化说明：
     * - 使用Spring Security标准认证流程，确保安全性
     * - 支持RememberMe功能，提升用户体验
     * - 完善的异常处理，区分不同类型的登录失败原因
     *
     * @param loginDTO 登录请求体
     * @return 登录结果（包含用户信息和JWT令牌）
     */
    @Override
    public R<UserLoginResponseDTO> login(LoginDTO loginDTO) {
        log.info("处理用户登录: 邮箱={}", loginDTO.getEmail());

        try {
            // 使用 Spring Security 的标准认证流程
            // 这会自动调用 AuthUserDetailsService.loadUserByUsername() 加载用户信息
            // 并自动进行密码验证、账户状态检查等操作
            UsernamePasswordAuthenticationToken authRequest = 
                new UsernamePasswordAuthenticationToken(
                    loginDTO.getEmail(), 
                    loginDTO.getPassword()
                );
            
            // 执行认证（内部会调用 loadUserByUsername 和密码验证）
            Authentication authentication = authenticationManager.authenticate(authRequest);

            // 认证成功会拿到用户详情
            LoginUserBody loginUser = (LoginUserBody) authentication.getPrincipal();

            // 生成 JWT Token
            boolean rememberMe = loginDTO.getRememberMe() != null ? loginDTO.getRememberMe() : false;
            TokenDTO tokenDTO = generateTokens(loginUser.getUserId(), rememberMe);

            // 生成 RememberMeToken,如果开了记住我
            String rememberMeToken = null;
            if (rememberMe) {
                rememberMeToken = createRememberMeToken(loginUser.getUserId());
            }

            // 构建响应 DTO
            // 优化：从LoginUserBody中获取头像信息，如果avatarUrl存在则使用，否则从avatarData生成
            String avatarData = null;
            if (loginUser.getAvatarUrl() != null) {
                avatarData = loginUser.getAvatarUrl();
            } else if (loginUser.getAvatarData() != null && loginUser.getAvatarData().length > 0) {
                // 从avatarData生成Base64 Data URL
                try {
                    String base64 = java.util.Base64.getEncoder().encodeToString(loginUser.getAvatarData());
                    String contentType = loginUser.getAvatarContentType() != null ? 
                            loginUser.getAvatarContentType() : "image/jpeg";
                    avatarData = "data:" + contentType + ";base64," + base64;
                } catch (Exception e) {
                    log.warn("生成头像Base64失败 - userId: {}", loginUser.getUserId(), e);
                }
            }

            UserDTO userDTO = UserDTO.builder()
                    .id(loginUser.getUserId())
                    .email(loginUser.getEmail())
                    .name(loginUser.getName())
                    .avatarData(avatarData)
                    .avatarContentType(loginUser.getAvatarContentType())
                    .title(loginUser.getTitle())
                    .institution(loginUser.getInstitution())
                    .roles(loginUser.getRoles())
                    .permissions(new ArrayList<>(loginUser.getPermissions()))
                    .build();

            UserLoginResponseDTO response = UserLoginResponseDTO.builder()
                    .user(userDTO)
                    .accessToken(tokenDTO.getAccessToken())
                    .refreshToken(tokenDTO.getRefreshToken())
                    .expiresIn(tokenDTO.getExpiresIn())
                    .tokenType(tokenDTO.getTokenType())
                    .rememberMe(rememberMe)
                    .rememberMeToken(rememberMeToken)
                    .build();

            log.info("用户登录成功 - 用户ID: {}, 邮箱: {}, 记住我: {}", 
                    loginUser.getUserId(), loginUser.getEmail(), rememberMe);
            
            return R.ok(response, "登录成功");

        } catch (BadCredentialsException e) {
            // 用户名或密码错误
            log.warn("登录失败: 邮箱或密码错误 - 邮箱: {}", loginDTO.getEmail());
            return R.fail("邮箱或密码错误");
            
        } catch (LockedException e) {
            // 账户被锁定
            log.warn("登录失败: 账户已被锁定 - 邮箱: {}", loginDTO.getEmail());
            return R.fail("账户已被锁定，请联系管理员");
            
        } catch (DisabledException e) {
            // 账户被禁用
            log.warn("登录失败: 账户已被禁用 - 邮箱: {}", loginDTO.getEmail());
            return R.fail("账户已被禁用");
            
        } catch (AuthenticationException e) {
            // 其他认证异常
            log.error("登录失败: 认证异常 - 邮箱: {}, 错误: {}", loginDTO.getEmail(), e.getMessage());
            return R.fail("登录失败：" + e.getMessage());
            
        } catch (Exception e) {
            log.error("登录异常 - 邮箱: {}, 错误: {}", loginDTO.getEmail(), e.getMessage(), e);
            return R.fail("登录失败，请稍后重试");
        }
    }

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    @Override
    public R<Boolean> checkEmail(String email) {
        log.info("检查邮箱: 邮箱={}", email);

        try {
            // 校验邮箱格式
            if (!isValidEmail(email)) {
                return R.fail("邮箱格式不正确");
            }

            boolean exists = userRepository.findByEmail(email).isPresent();
            return exists ? R.fail("邮箱已被注册") : R.ok(true, "邮箱可用");

        } catch (Exception e) {
            log.error("检查邮箱异常 - 邮箱: {}, 错误: {}", email, e.getMessage(), e);
            return R.fail("检查邮箱失败，请稍后重试");
        }
    }

    /**
     * 发送验证码
     *
     * @param verificationCodeDTO 验证码请求体
     * @return 发送结果
     */
    @Override
    public R<Void> sendVerificationCode(VerificationCodeDTO verificationCodeDTO) {
        try{
            VerificationCodeType type = VerificationCodeType.valueOf(verificationCodeDTO.getType().toUpperCase());
            return verificationCodeService.generateAndSendCode(verificationCodeDTO.getEmail(), type);
        }catch (ServiceException e){
            log.error("发送验证码失败 - 邮箱: {}, 类型: {}, 错误: {}",
                    verificationCodeDTO.getEmail(), verificationCodeDTO.getType(), e.getMessage(), e);
            return R.fail("发送验证码失败，请稍后重试");
        }
    }

    /**
     * 验证验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @param type  验证码类型
     * @return 验证结果
     */
    @Override
    public R<Boolean> verifyCode(String email, String code, String type) {
        try {
            VerificationCodeType codeType = VerificationCodeType.valueOf(type.toUpperCase());
            R<Boolean> result = verificationCodeService.validateCode(email, code, codeType);

            // 如果验证失败，返回具体的错误信息
            if (R.isSuccess(result) && Boolean.FALSE.equals(result.getData())) {
                return R.fail(result.getMsg() != null ? result.getMsg() : "验证码错误或已过期");
            }

            return result;
        } catch (IllegalArgumentException e) {
            log.error("验证码类型错误 - 类型: {}, 错误: {}", type, e.getMessage());
            return R.fail("验证码类型错误");
        } catch (ServiceException e) {
            log.error("验证验证码失败 - 邮箱: {}, 类型: {}, 错误: {}", email, type, e.getMessage(), e);
            return R.fail("验证验证码失败，请稍后重试");
        }
    }

    /**
     * 生成JWT令牌
     * 访问令牌和刷新令牌
     * 根据用户ID和"记住我"选项生成不同过期时间的令牌
     *
     * @param userId     用户ID
     * @param rememberMe 是否记住我
     * @return token信息
     */
    @Override
    public TokenDTO generateTokens(Long userId, boolean rememberMe) {
        try {
            // 查询用户信息以获取 email
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                throw new RuntimeException("用户不存在，无法生成令牌");
            }
            String email = userOptional.get().getEmail();

            // 获取用户角色信息
            java.util.List<String> roles = authUserDetailsService.getUserRoles(userId);
            String rolesStr = String.join(",", roles);

            // 根据记住我选项确定过期时间（分钟）
            // 访问令牌过期时间：始终保持短期有效（30分钟），不受rememberMe影响
            int accessTokenExpireMinutes = TokenConstants.DEFAULT_ACCESS_TOKEN_EXPIRE_MINUTES;

            // 刷新令牌过期时间：根据rememberMe调整
            // 不记住我：3天（4320分钟）,记住我：30天（43200分钟）
            int refreshTokenExpireMinutes = rememberMe ?
                    TokenConstants.REMEMBER_ME_REFRESH_TOKEN_EXPIRE_MINUTES : TokenConstants.DEFAULT_REFRESH_TOKEN_EXPIRE_MINUTES;

            // 创建自定义 claims，包含 userId、email 和 roles
            Map<String, Object> claims = new HashMap<>();
            claims.put(TokenConstants.JWT_CLAIM_USER_ID, userId);
            claims.put(TokenConstants.JWT_CLAIM_EMAIL, email);
            claims.put(TokenConstants.JWT_CLAIM_ROLES, rolesStr);

            // 生成访问令牌（subject 使用 userId，claims 包含 email）
            String accessToken = jwtUtils.createToken(userId.toString(), accessTokenExpireMinutes, claims);

            // 生成刷新令牌（长期有效，用于获取新的访问令牌）
            String refreshToken = jwtUtils.createToken(userId.toString(), refreshTokenExpireMinutes, claims);

            log.info("=== 生成的Token信息 ===");
            log.info("用户ID: {}", userId);
            log.info("用户角色: {}", rolesStr);
            log.info("访问Token: {}", accessToken);
            log.info("刷新Token: {}", refreshToken);

            // 构建令牌DTO对象
            TokenDTO tokenDTO = new TokenDTO();
            tokenDTO.setAccessToken(accessToken);
            tokenDTO.setRefreshToken(refreshToken);
            tokenDTO.setTokenType(TokenConstants.TOKEN_TYPE_BEARER);
            // 转换为秒
            tokenDTO.setExpiresIn((long) accessTokenExpireMinutes * 60);

            // 将访问令牌存储到Redis，用于后续校验和管理
            String tokenKey = CacheConstants.USER_TOKEN_PREFIX + userId;
            long cacheTimeSeconds = (long) accessTokenExpireMinutes * 60;
            redisService.setCacheObject(tokenKey, accessToken, cacheTimeSeconds, TimeUnit.SECONDS);

            log.info("JWT令牌生成成功 - 用户ID: {}, 记住我: {}", userId, rememberMe);
            return tokenDTO;

        } catch (Exception e) {
            log.error("生成JWT令牌失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new RuntimeException("生成令牌失败");
        }
    }

    /**
     * 刷新JWT令牌
     * 根据传入的refreshToken，生成新的访问令牌 Access Token
     * 生成新AccessToken时包含完整的Claims信息（用户角色、邮箱等）
     *
     * @param refreshToken 刷新令牌
     * @return 新的token信息
     */
    @Override
    public TokenDTO refreshToken(String refreshToken) {
        try {
            // 1. 校验 Refresh Token 格式和签名
            if (!jwtUtils.validateToken(refreshToken)) {
                throw new RuntimeException("无效的Refresh Token");
            }

            // 2. 校验是否在黑名单
            if (isTokenBlacklisted(refreshToken)) {
                throw new RuntimeException("Refresh Token已失效");
            }

            // 3. 解析用户ID
            String userIdStr = jwtUtils.parseToken(refreshToken);
            if (userIdStr == null) {
                throw new RuntimeException("无法解析用户ID");
            }
            Long userId = Long.valueOf(userIdStr);

            // 4. 判断 Refresh Token 是否过期
            Long remainingTime = jwtUtils.getRemainingTime(refreshToken);
            if (remainingTime == null || remainingTime <= 0) {
                throw new RuntimeException("Refresh Token已过期");
            }

            // 5. 查询用户信息以获取完整的claims数据
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("用户不存在 - 用户ID: {}", userId);
                throw new RuntimeException("用户不存在");
            }

            User user = userOptional.get();
            // 检查用户状态
            if (user.getIsDeleted()) {
                log.warn("用户已被删除 - 用户ID: {}", userId);
                throw new RuntimeException("用户不存在");
            }
            if (user.getIsLocked()) {
                log.warn("用户已被锁定 - 用户ID: {}", userId);
                throw new RuntimeException("账号已被锁定");
            }

            String email = user.getEmail();

            // 6. 获取用户角色信息（关键：保留权限信息）
            java.util.List<String> roles = authUserDetailsService.getUserRoles(userId);
            String rolesStr = String.join(",", roles);

            // 7. 创建包含完整信息的claims
            Map<String, Object> claims = new HashMap<>();
            claims.put(TokenConstants.JWT_CLAIM_USER_ID, userId);
            claims.put(TokenConstants.JWT_CLAIM_EMAIL, email);
            claims.put(TokenConstants.JWT_CLAIM_ROLES, rolesStr);  // 关键：包含角色信息

            // 8. 生成新的Access Token（带完整Claims）
            int accessTokenExpireMinutes = TokenConstants.DEFAULT_ACCESS_TOKEN_EXPIRE_MINUTES;
            String newAccessToken = jwtUtils.createToken(userIdStr, accessTokenExpireMinutes, claims);

            // 9. 存储新的Access Token到Redis
            String tokenKey = CacheConstants.USER_TOKEN_PREFIX + userId;
            long cacheTimeSeconds = (long) accessTokenExpireMinutes * 60;
            redisService.setCacheObject(tokenKey, newAccessToken, cacheTimeSeconds, TimeUnit.SECONDS);

            // 10. 如果用户有RememberMe token，刷新其过期时间（保持30天有效期）
            try {
                refreshRememberMeToken(userId);
                log.debug("已刷新用户 {} 的RememberMe token过期时间", userId);
            } catch (Exception e) {
                // RememberMe token刷新失败不影响token刷新，只记录日志
                log.debug("刷新RememberMe token失败（可能用户未勾选记住我）: {}", e.getMessage());
            }

            // 11. 返回新的TokenDTO（Refresh Token保持不变）
            TokenDTO tokenDTO = new TokenDTO();
            tokenDTO.setAccessToken(newAccessToken);
            tokenDTO.setRefreshToken(refreshToken); // 保持不变
            tokenDTO.setTokenType(TokenConstants.TOKEN_TYPE_BEARER);
            tokenDTO.setExpiresIn(cacheTimeSeconds);

            log.info("刷新Token成功 - 用户ID: {}, 角色: {}", userId, rolesStr);
            return tokenDTO;

        } catch (Exception e) {
            log.error("刷新Token失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("刷新Token失败");
        }
    }

    /**
     * 验证JWT令牌
     * 检查令牌是否合法、未过期，并解析出用户ID
     *
     * @param token 待验证的JWT令牌
     * @return 验证通过返回用户ID，否则返回null
     */
    @Override
    public String validateToken(String token) {
        try {
            // 先通过JWT工具类验证令牌格式和签名
            if (!jwtUtils.validateToken(token)) {
                return null;
            }

            // 解析令牌获取用户ID字符串
            return jwtUtils.parseToken(token);

        } catch (Exception e) {
            log.debug("JWT令牌验证失败 - token: {}, 错误: {}", token, e.getMessage());
            return null;
        }
    }

    /**
     * 将token加入黑名单，并清除用户的缓存
     *
     * @param token  JWT令牌
     * @param userId 用户ID
     */
    @Override
    public void blacklistToken(String token, Long userId) {
        if (StringUtils.isBlank(token) || userId == null) {
            log.warn("加入黑名单失败 - token 或 userId 为空");
            return;
        }

        try {
            // 计算 token 剩余有效时间
            Long remainingTime = jwtUtils.getRemainingTime(token);
            if (remainingTime != null && remainingTime > 0) {
                String blacklistKey = CacheConstants.TOKEN_BLACKLIST_PREFIX + token;
                redisService.setCacheObject(blacklistKey, userId.toString(), remainingTime, TimeUnit.SECONDS);
                log.debug("Token 已加入黑名单 - 用户ID: {}, 剩余有效期: {} 秒", userId, remainingTime);
            }

            // 清除用户的 token 缓存（保证后续不会再查到）
            String userTokenKey = CacheConstants.USER_TOKEN_PREFIX + userId;
            redisService.deleteObject(userTokenKey);

            log.info("Token 已失效并加入黑名单 - 用户ID: {}", userId);

        } catch (Exception e) {
            log.error("加入 token 黑名单失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 检查token是否在黑名单中
     *
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    @Override
    public boolean isTokenBlacklisted(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        try {
            String blacklistKey = CacheConstants.TOKEN_BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisService.hasKey(blacklistKey));
        } catch (Exception e) {
            log.warn("检查 token 黑名单状态失败 - token: {}, 错误: {}", token, e.getMessage());
            return false;
        }
    }

    /**
     * 验证令牌并返回详细验证结果
     * 提供Token验证的详细信息，包括有效性、用户ID、角色、剩余时间等
     *
     * @param token JWT令牌
     * @return 验证结果（包含详细信息）
     */
    @Override
    public R<TokenValidateResponseDTO> validateTokenWithDetails(String token) {
        TokenValidateResponseDTO response = new TokenValidateResponseDTO();

        try {
            // 移除Bearer前缀（如果服务层被直接调用，可能没有前缀）
            String cleanToken = removeBearerPrefix(token);

            // 1. 检查Token是否在黑名单中
            if (isTokenBlacklisted(cleanToken)) {
                response.setValid(false);
                response.setMessage("令牌已失效");
                log.debug("令牌验证失败 - 在黑名单中");
                return R.ok(response);
            }

            // 2. 校验JWT Token签名和有效期
            String userId = validateToken(cleanToken);
            if (userId == null) {
                response.setValid(false);
                response.setMessage("令牌无效或已过期");
                log.debug("令牌验证失败 - JWT验证不通过");
                return R.ok(response);
            }

            // 3. 获取令牌剩余时间和角色信息
            Long remainingTime = jwtUtils.getRemainingTime(cleanToken);
            Claims claims = jwtUtils.getClaims(cleanToken);
            String rolesStr = null;
            if (claims != null) {
                rolesStr = (String) claims.get(TokenConstants.JWT_CLAIM_ROLES);
            }

            // 4. 构建成功响应
            response.setValid(true);
            response.setUserId(userId);
            response.setRoles(rolesStr);
            response.setRemainingTime(remainingTime);
            response.setMessage("令牌有效");

            log.debug("令牌验证成功 - 用户ID: {}, 角色: {}, 剩余时间: {}秒", userId, rolesStr, remainingTime);
            return R.ok(response, "令牌有效");

        } catch (Exception e) {
            log.error("令牌验证异常", e);
            response.setValid(false);
            response.setMessage("令牌验证异常");
            return R.ok(response);
        }
    }

    /**
     * 重置密码
     *
     * @param request 重置密码请求
     * @return 操作结果
     */
    @Override
    public R<Void> resetPassword(ResetPasswordDTO request) {
        log.info("处理重置密码请求 - 邮箱: {}", request.getEmail());

        try {
            // 1. 查找用户
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                return R.fail("该邮箱未注册");
            }
            User user = userOpt.get();

            // 1. 校验验证码
            R<Boolean> validResult = verificationCodeService.validateCode(
                    request.getEmail(), request.getVerificationCode(), VerificationCodeType.RESET_PASSWORD
            );
            if (!R.isSuccess(validResult) || Boolean.FALSE.equals(validResult.getData())) {
                return R.fail("验证码错误或已过期");
            }

            // 2. 校验两次密码一致性
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return R.fail("两次输入的密码不一致");
            }

            // 3. 校验密码合法性及其强度
            if (!PasswordUtils.isValidPassword(request.getNewPassword())) {
                return R.fail("密码必须为6-16位字母和数字组合");
            }
            validatePasswordStrength(request.getNewPassword());

            // 4. 更新用户密码
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // 5. 将旧 token 加入黑名单并清理（强制重新登录）
            String userTokenKey = CacheConstants.USER_TOKEN_PREFIX + user.getId();
            String oldToken = redisService.getCacheObject(userTokenKey);
            if (oldToken != null) {
                blacklistToken(oldToken, user.getId());
            }

            // 6. 删除用户的RememberMe token（强制重新登录）
            try {
                rememberMeTokenRepository.deleteByUserId(user.getId());
            } catch (Exception e) {
                log.warn("删除RememberMe token失败 - userId: {}", user.getId(), e);
            }

            log.info("用户密码重置成功 - 用户ID: {}, 邮箱: {}", user.getId(), user.getEmail());
            return R.ok(null, "密码重置成功，请重新登录");

        } catch (Exception e) {
            log.error("重置密码失败 - 邮箱: {}, 错误: {}", request.getEmail(), e.getMessage(), e);
            return R.fail("密码重置失败，请稍后重试");
        }
    }

    /**
     * 忘记密码
     *
     * @param request 忘记密码请求
     * @return 操作结果
     */
    @Override
    public R<Void> forgotPassword(ForgetPasswordDTO request) {
        log.info("处理忘记密码请求 - 邮箱: {}", request.getEmail());

        try {
            // 1. 查找用户
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                return R.fail("该邮箱未注册");
            }
            User user = userOpt.get();

            // 2. 校验验证码
            R<Boolean> validResult = verificationCodeService.validateCode(
                    request.getEmail(), request.getVerificationCode(), VerificationCodeType.RESET_PASSWORD
            );
            if (!R.isSuccess(validResult) || Boolean.FALSE.equals(validResult.getData())) {
                return R.fail("验证码错误或已过期");
            }

            // 3. 校验新密码合法性及其强度
            if (!PasswordUtils.isValidPassword(request.getNewPassword())) {
                return R.fail("密码必须为6-16位字母和数字组合");
            }
            validatePasswordStrength(request.getNewPassword());

            // 4. 更新用户密码
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // 5. 将旧 token 加入黑名单并清理
            String userTokenKey = CacheConstants.USER_TOKEN_PREFIX + user.getId();
            String oldToken = redisService.getCacheObject(userTokenKey);
            if (oldToken != null) {
                blacklistToken(oldToken, user.getId());
            }

            log.info("用户密码重置成功 - 用户ID: {}, 邮箱: {}", user.getId(), user.getEmail());
            return R.ok(null, "密码重置成功，请重新登录");

        } catch (Exception e) {
            log.error("重置密码失败 - 邮箱: {}, 错误: {}", request.getEmail(), e.getMessage(), e);
            return R.fail("密码重置失败，请稍后重试");
        }
    }

    /**
     * 修改用户邮箱（改绑邮箱）
     * 验证新邮箱的验证码和当前密码后，更新用户邮箱地址
     *
     * @param changeEmailDTO 修改邮箱表单数据（包含当前密码、新邮箱、验证码）
     * @return 成功返回更新后的用户信息；失败返回错误信息（如验证码无效、邮箱已被使用等）
     */
    @Override
    @Transactional
    public R<UserDTO> changeEmail(ChangeEmailDTO changeEmailDTO) {
        Long userId = changeEmailDTO.getUserId();
        log.info("处理修改用户邮箱请求 - 用户ID: {}, 新邮箱: {}", userId, changeEmailDTO.getNewEmail());

        try {
            // 1. 验证用户是否存在
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }
            User user = userOpt.get();

            // 2. 验证当前密码
            if (changeEmailDTO.getCurrentPassword() != null &&
                !passwordEncoder.matches(changeEmailDTO.getCurrentPassword(), user.getPasswordHash())) {
                return R.fail("当前密码错误");
            }

            // 3. 检查新邮箱是否已被使用
            Optional<User> existingUser = userRepository.findByEmail(changeEmailDTO.getNewEmail());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                return R.fail("该邮箱已被使用");
            }

            // 4. 校验新邮箱的验证码
            R<Boolean> validResult = verificationCodeService.validateCode(
                    changeEmailDTO.getNewEmail(),
                    changeEmailDTO.getVerificationCode(),
                    VerificationCodeType.CHANGE_EMAIL
            );
            if (!R.isSuccess(validResult) || Boolean.FALSE.equals(validResult.getData())) {
                return R.fail("验证码错误或已过期");
            }

            // 5. 更新用户邮箱
            String oldEmail = user.getEmail();
            user.setEmail(changeEmailDTO.getNewEmail());
            userRepository.save(user);

            // 6. 将旧 token 加入黑名单并清理（强制重新登录）
            String userTokenKey = CacheConstants.USER_TOKEN_PREFIX + user.getId();
            String oldToken = redisService.getCacheObject(userTokenKey);
            if (oldToken != null) {
                blacklistToken(oldToken, user.getId());
            }

            // 7. 删除用户的RememberMe token（强制重新登录）
            try {
                rememberMeTokenRepository.deleteByUserId(userId);
            } catch (Exception e) {
                log.warn("删除RememberMe token失败 - userId: {}", userId, e);
            }

            // 8. 构建返回DTO
            UserDTO userDTO = userConverter.toDTO(user);

            log.info("用户邮箱修改成功 - 用户ID: {}, 旧邮箱: {}, 新邮箱: {}",
                    userId, oldEmail, changeEmailDTO.getNewEmail());
            return R.ok(userDTO, "邮箱修改成功，请重新登录");

        } catch (Exception e) {
            log.error("修改用户邮箱失败 - 用户ID: {}, 新邮箱: {}, 错误: {}",
                    userId, changeEmailDTO.getNewEmail(), e.getMessage(), e);
            return R.fail("修改邮箱失败，请稍后重试");
        }
    }

    /**
     * 用户登出
     *
     * @param tokenHeader 令牌头
     * @return 操作结果
     */
    @Override
    public R<Void> logout(String tokenHeader) {
        if (StringUtils.isBlank(tokenHeader) ||
                !tokenHeader.startsWith(TokenConstants.TOKEN_TYPE_BEARER + " ")) {
            return R.fail("无效的Authorization头");
        }

        // 去掉 "Bearer " 前缀
        String token = tokenHeader.substring((TokenConstants.TOKEN_TYPE_BEARER + " ").length());

        try {
            // 1. 验证 token
            String userIdStr = validateToken(token);
            if (userIdStr == null) {
                return R.fail("无效的Token");
            }
            Long userId = Long.valueOf(userIdStr);

            // 2. 将 token 加入黑名单并清理缓存
            blacklistToken(token, userId);

            // 3. 删除用户的RememberMe token（如果存在）
            try {
                rememberMeTokenRepository.deleteByUserId(userId);
            } catch (Exception e) {
                log.warn("删除RememberMe token失败 - userId: {}", userId, e);
            }

            log.info("用户登出成功 - 用户ID: {}", userId);
            return R.ok(null, "登出成功");

        } catch (Exception e) {
            log.error("用户登出失败 - 错误: {}", e.getMessage(), e);
            return R.fail("登出失败");
        }
    }

    /**
     * 创建RememberMe token
     *
     * @param userId 用户id
     * @return token
     */
    @Override
    @Transactional
    public String createRememberMeToken(Long userId) {
        log.debug("为用户 {} 创建RememberMe token", userId);

        // 删除旧token
        rememberMeTokenRepository.deleteByUserId(userId);

        // 创建新token
        String token = UUID.randomUUID().toString().replace("-", "");
        RememberMeToken entity = RememberMeToken.builder()
                .userId(userId)
                .token(token)
                .expiryTime(LocalDateTime.now().plusDays(REMEMBER_ME_DAYS))
                .createdTime(LocalDateTime.now())
                .build();

        rememberMeTokenRepository.save(entity);
        log.debug("为用户 {} 创建RememberMe token成功", userId);
        return token;
    }

    /**
     * 验证RememberMe token
     *
     * @param token RememberToken
     * @return 验证结果
     */
    @Override
    public Optional<Long> validateRememberMeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        return rememberMeTokenRepository.findByToken(token)
                .filter(v -> {
                    boolean isValid = v.getExpiryTime().isAfter(LocalDateTime.now());
                    if (!isValid) {
                        log.debug("RememberMe token已过期: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
                    }
                    return isValid;
                })
                .map(v -> {
                    log.debug("RememberMe token验证成功，用户ID: {}", v.getUserId());
                    return v.getUserId();
                });
    }

    /**
     * 刷新RememberMe token过期时间
     * 在用户活跃时延长token的有效期
     *
     * @param userId 用户ID
     */
    @Override
    public void refreshRememberMeToken(Long userId) {
        rememberMeTokenRepository.findByUserId(userId).ifPresent(token -> {
            token.setExpiryTime(LocalDateTime.now().plusDays(REMEMBER_ME_DAYS));
            rememberMeTokenRepository.save(token);
            log.debug("刷新用户 {} 的RememberMe token过期时间", userId);
        });
    }


    /**
     * 删除指定的RememberMe token
     *
     * @param token RememberToken
     */
    @Override
    @Transactional
    public void deleteRememberMeToken(String token) {
        rememberMeTokenRepository.findByToken(token).ifPresent(entity -> {
            rememberMeTokenRepository.delete(entity);
            log.debug("删除RememberMe token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
        });
    }


    /**
     * 清理过期的RememberMe token
     */
    @Override
    @Transactional
    public void cleanExpiredTokens() {
        int deletedCount = rememberMeTokenRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("清理了 {} 个过期的RememberMe token", deletedCount);
        }
    }


    /**
     * 为新用户分配默认角色
     * 注册时自动分配 USER 角色（普通用户）
     *
     * @param userId 用户ID
     */
    private void assignDefaultRoleToUser(Long userId) {
        try {
            log.info("========================================");
            log.info("开始为新用户分配默认系统角色: userId={}", userId);
            log.info("========================================");

            // 查找 USER 角色（系统级普通用户角色）
            log.info("步骤1：查找 USER 角色...");
            R<Long> roleResult = roleService.getRoleIdByName("USER");

            log.info("步骤1结果 - 查询角色: success={}, data={}",
                    R.isSuccess(roleResult), roleResult.getData());

            if (R.isSuccess(roleResult) && roleResult.getData() != null) {
                Long roleId = roleResult.getData();
                log.info("步骤2：找到 USER 角色，ID={}", roleId);

                // 分配角色给用户
                log.info("步骤3：调用 assignRolesToUser, userId={}, roleIds=[{}]", userId, roleId);
                R<Void> assignResult = roleService.assignRolesToUser(userId, java.util.List.of(roleId));

                log.info("步骤3结果 - 分配角色: success={}, msg={}",
                        R.isSuccess(assignResult), assignResult.getMsg());

                if (R.isSuccess(assignResult)) {
                    log.info("========================================");
                    log.info("✅ 成功为用户分配默认角色 USER: userId={}", userId);
                    log.info("========================================");
                } else {
                    log.error("========================================");
                    log.error("❌ 为用户分配默认角色失败: userId={}, 错误={}", userId, assignResult.getMsg());
                    log.error("========================================");
                }
            } else {
                log.error("========================================");
                log.error("❌ 未找到 USER 角色，无法分配默认角色");
                log.error("userId={}, roleResult={}", userId, roleResult);
                log.error("提示：请检查 roles 表是否存在 name='USER' 的记录");
                log.error("========================================");
            }

        } catch (Exception e) {
            // 角色分配失败不应该影响注册流程
            // 管理员可以后续手动分配角色
            log.error("========================================");
            log.error("❌ 为用户分配默认角色发生异常");
            log.error("userId={}, 异常类型={}, 错误信息={}", userId, e.getClass().getName(), e.getMessage());
            log.error("堆栈跟踪:", e);
            log.error("========================================");
        }
    }

    /**
     * 密码强度校验
     */
    private void validatePasswordStrength(String password) {
        if (!PasswordUtils.isValidPassword(password)) {
            throw new ServiceException("密码必须为6-16位");
        }

        // 可根据需求添加密码强度建议
        int strength = PasswordUtils.validatePasswordStrength(password);
        if (strength < 2) {
            log.warn("密码强度较弱 - 建议使用字母、数字和特殊字符组合");
        }
    }

    /**
     * 邮箱格式验证
     */
    private boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 去掉 Bearer 前缀（如果有的话）
     */
    private String removeBearerPrefix(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }
        return token.trim();
    }

    /**
     * 获取密码强度描述
     * 
     * @param strength 密码强度等级（0-无效，1-弱，2-中等，3-强）
     * @return 密码强度描述
     */
    private String getPasswordStrengthDescription(int strength) {
        switch (strength) {
            case 3:
                return "强";
            case 2:
                return "中等";
            case 1:
                return "弱";
            default:
                return "无效";
        }
    }
}
