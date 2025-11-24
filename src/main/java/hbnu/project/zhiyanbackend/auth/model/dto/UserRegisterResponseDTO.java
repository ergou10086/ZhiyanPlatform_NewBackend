package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.*;

/**
 * 用户注册响应DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterResponseDTO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户邮箱
     */
    private String email;
    
    /**
     * 用户姓名
     */
    private String name;
    
    /**
     * 用户职称
     */
    private String title;
    
    /**
     * 所属机构
     */
    private String institution;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 令牌类型
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 密码强度
     */
    private String passwordStrength;
    
    /**
     * 记住我状态
     */
    private Boolean rememberMe;
}

