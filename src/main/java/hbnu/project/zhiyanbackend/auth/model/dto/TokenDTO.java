package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.*;

/**
 * JWT令牌数据传输对象
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDTO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserDTO user;
}