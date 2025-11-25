package hbnu.project.zhiyanbackend.security.filter;

import hbnu.project.zhiyanbackend.auth.service.AuthService;
import hbnu.project.zhiyanbackend.auth.service.impl.AuthUserDetailsServiceImpl;
import hbnu.project.zhiyanbackend.basic.constants.TokenConstants;
import hbnu.project.zhiyanbackend.basic.utils.JwtUtils;
import hbnu.project.zhiyanbackend.security.context.LoginUserBody;
import hbnu.project.zhiyanbackend.security.context.SecurityContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT认证过滤器
 * 用于验证JWT token并设置认证信息
 * 在请求处理过程中验证 JWT 令牌并设置认证信息
 * 
 * 功能说明：
 * 1. 从请求头或参数中提取JWT token
 * 2. 验证token的有效性和黑名单状态
 * 3. 解析token获取用户信息并设置到Spring Security上下文
 * 4. 支持RememberMe token认证（作为JWT认证的备选方案）
 * 
 * 优化说明：
 * - 启用token黑名单检查，提升安全性
 * - RememberMe认证时加载完整用户信息（角色、权限等）
 * - 完善的错误处理和日志记录
 * - 确保与AuthService和AuthUserDetailsService的正确集成
 *
 * @author akoiv
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
// 继承OncePerRequestFilter，确保每个请求只被过滤一次
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final AuthUserDetailsServiceImpl authUserDetailsService;

    /**
     * 从Authorization头提取token
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            // 移除"Bearer "前缀
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 从请求中提取RememberMe的token
     */
    private String extractRememberMeToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("remember_me_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 处理每个请求的认证逻辑
     * 
     * 认证流程：
     * 1. 优先尝试JWT token认证（从Authorization头或请求参数）
     * 2. 如果JWT认证失败，尝试RememberMe token认证（从Cookie）
     * 3. 认证成功后设置Spring Security上下文和自定义上下文
     * 4. 认证失败时清理上下文，避免残留无效信息
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // 记录特定请求（如 Coze API 请求）用于调试
        if (requestURI.contains("/api/coze")) {
            log.debug("[JWT Filter] 处理 Coze API 请求: {} {}, URI: {}", method, requestURI, requestURI);
            log.debug("[JWT Filter] Authorization Header: {}", request.getHeader("Authorization") != null ? "存在" : "不存在");
        }
        
        try {
            boolean authenticated = false;
            
            // ==================== 1. JWT Token 认证 ====================
            // 1.1 从请求参数或Authorization头提取JWT token
            String token = ServletRequestUtils.getStringParameter(request, "token");
            if (StringUtils.isBlank(token)) {
                token = extractTokenFromHeader(request);
            }

            // 1.2 检查JWT token是否存在且有效
            if (StringUtils.isNotBlank(token) && jwtUtils.validateToken(token)) {
                
                // 1.3 检查token是否在黑名单中（关键安全检查）
                if (authService.isTokenBlacklisted(token)) {
                    log.debug("Token已在黑名单中，拒绝认证: {}", maskToken(token));
                    clearSecurityContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 1.4 解析token获取用户信息
                Claims claims = jwtUtils.getClaims(token);
                if (claims != null) {
                    // 1.5 从token的载荷(claims)中提取用户信息
                    String userIdStr = String.valueOf(claims.get(TokenConstants.JWT_CLAIM_USER_ID));
                    // 从Subject中获取邮箱
                    String email = claims.getSubject();
                    String rolesStr = (String) claims.get(TokenConstants.JWT_CLAIM_ROLES);
                    
                    // 1.6 验证提取的用户信息是否有效
                    if (StringUtils.isNotBlank(userIdStr) && StringUtils.isNotBlank(email)) {
                        try {
                            Long userId = Long.valueOf(userIdStr);
                            
                            // 1.7 从数据库加载完整用户信息（包含角色、权限等）
                            // 使用AuthUserDetailsService加载完整用户信息，而不是仅从token中获取
                            UserDetails userDetails = authUserDetailsService.loadUserById(userId);
                            
                            if (userDetails instanceof LoginUserBody loginUser) {

                                // 1.8 设置到Spring Security上下文和自定义上下文
                                setAuthenticationContext(loginUser, request);
                                
                                authenticated = true;
                                log.debug("JWT认证成功 - 用户ID: {}, 邮箱: {}, 角色数: {}", 
                                        userId, email, loginUser.getRoles() != null ? loginUser.getRoles().size() : 0);
                            } else {
                                log.warn("加载的用户详情不是LoginUserBody类型 - userId: {}", userId);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("用户ID格式错误: {}", userIdStr);
                        } catch (UsernameNotFoundException e) {
                            log.warn("用户不存在: {}", e.getMessage());
                        } catch (Exception e) {
                            log.error("加载用户详情失败 - userId: {}", userIdStr, e);
                        }
                    } else {
                        log.debug("Token中缺少必要的用户信息 - userId: {}, email: {}", userIdStr, email);
                    }
                }
            }
            
            // ==================== 2. RememberMe Token 认证（备选方案）====================
            // 如果JWT认证失败，尝试RememberMe token认证
            if (!authenticated) {
                String rememberToken = extractRememberMeToken(request);
                if (StringUtils.isNotBlank(rememberToken)) {
                    Optional<Long> userIdOpt = authService.validateRememberMeToken(rememberToken);
                    if (userIdOpt.isPresent()) {
                        Long userId = userIdOpt.get();
                        try {
                            // 加载完整用户信息，而不是构建简化对象
                            UserDetails userDetails = authUserDetailsService.loadUserById(userId);
                            
                            if (userDetails instanceof LoginUserBody) {
                                LoginUserBody loginUser = (LoginUserBody) userDetails;
                                
                                // 设置认证上下文
                                setAuthenticationContext(loginUser, request);
                                
                                // 刷新RememberMe token过期时间（用户活跃时延长有效期）
                                try {
                                    authService.refreshRememberMeToken(userId);
                                } catch (Exception e) {
                                    log.warn("刷新RememberMe token失败 - userId: {}", userId, e);
                                }
                                
                                authenticated = true;
                                log.debug("RememberMe认证成功 - 用户ID: {}, 邮箱: {}, 角色数: {}", 
                                        userId, loginUser.getEmail(), 
                                        loginUser.getRoles() != null ? loginUser.getRoles().size() : 0);
                            } else {
                                log.warn("加载的用户详情不是LoginUserBody类型 - userId: {}", userId);
                            }
                        } catch (UsernameNotFoundException e) {
                            log.warn("RememberMe token关联的用户不存在: {}", e.getMessage());
                            // 用户不存在，删除无效的RememberMe token
                            try {
                                authService.deleteRememberMeToken(rememberToken);
                            } catch (Exception ex) {
                                log.warn("删除无效RememberMe token失败", ex);
                            }
                        } catch (Exception e) {
                            log.error("RememberMe认证失败 - userId: {}", userId, e);
                        }
                    } else {
                        log.debug("RememberMe token无效或已过期");
                    }
                }
            }
            
        } catch (Exception e) {
            // 认证过程中发生异常时输出日志
            log.error("JWT过滤器处理异常: {}", e.getMessage(), e);
            // 认证失败时清理上下文，避免残留无效信息
            clearSecurityContext();
        }

        // 继续过滤器链，让请求进入下一个过滤器或目标资源
        filterChain.doFilter(request, response);
    }


    /**
     * 设置认证上下文
     * 将用户认证信息设置到Spring Security上下文和自定义上下文
     * 
     * 优化说明：
     * - 使用LoginUserBody中的完整权限信息，而不是默认角色
     * - 确保权限信息正确设置到Spring Security上下文
     *
     * @param loginUser 登录用户信息
     * @param request HTTP请求对象
     */
    private void setAuthenticationContext(LoginUserBody loginUser, HttpServletRequest request) {
        // 从LoginUserBody中获取权限集合（已包含角色和权限）
        // LoginUserBody.getAuthorities()方法会自动构建包含角色和权限的GrantedAuthority集合
        Collection<? extends GrantedAuthority> authorities = loginUser.getAuthorities();
        
        // 如果authorities为空，至少添加一个默认角色（防止权限检查失败）
        if (authorities == null || authorities.isEmpty()) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            log.warn("用户权限为空，使用默认角色 - userId: {}", loginUser.getUserId());
        }

        // 创建认证令牌，包含用户信息、凭证（null）、权限列表
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                loginUser,
                null, // 凭证为null（JWT认证不需要密码）
                authorities
        );

        // 设置认证详情，如请求IP、会话ID等
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 将认证信息设置到Spring Security的上下文中
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authToken);

        // 设置到自定义上下文，方便业务代码中获取当前登录用户
        SecurityContextHolder.setLoginUser(loginUser);
    }


    /**
     * 清理安全上下文
     * 清除Spring Security上下文和自定义上下文中的认证信息
     */
    private void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        SecurityContextHolder.remove();
    }


    /**
     * 掩码Token用于日志记录
     * 只显示token的前后部分，中间用...代替，保护敏感信息
     *
     * @param token JWT token
     * @return 掩码后的token字符串
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 20) {
            return "***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }
}
