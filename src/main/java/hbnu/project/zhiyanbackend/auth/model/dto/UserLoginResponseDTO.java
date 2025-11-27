package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 用户登录响应DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应体")
public class UserLoginResponseDTO {
    
    /**
     * 用户信息
     */
    @Schema(description = "用户基本信息")
    private UserDTO user;
    
    /**
     * 访问令牌
     */
    @Schema(description = "访问令牌（JWT）", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    @Schema(description = "刷新令牌（用于刷新访问令牌）", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    /**
     * 令牌过期时间（秒）
     */
    @Schema(description = "访问令牌过期时间（秒）", example = "86400")
    private Long expiresIn;
    
    /**
     * 令牌类型
     */
    @Schema(description = "令牌类型", example = "Bearer", defaultValue = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 是否记住我
     */
    @Schema(description = "是否启用记住我", example = "false")
    private Boolean rememberMe;
    
    /**
     * 记住我令牌
     */
    @Schema(description = "记住我令牌（用于自动登录）", example = "remember_me_token_string...")
    private String rememberMeToken;
}

