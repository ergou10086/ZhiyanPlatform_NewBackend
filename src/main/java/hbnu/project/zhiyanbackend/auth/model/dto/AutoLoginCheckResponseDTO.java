package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.*;

/**
 * 自动登录检查响应
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoLoginCheckResponseDTO {

    /**
     * 是否可以自动登录
     */
    private Boolean canAutoLogin;

    /**
     * 用户ID（如果可以自动登录）
     */
    private Long userId;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 创建无token的响应
     */
    public static AutoLoginCheckResponseDTO noToken() {
        return AutoLoginCheckResponseDTO.builder()
                .canAutoLogin(false)
                .message("无有效的RememberMe token")
                .build();
    }

    /**
     * 创建有效token的响应
     */
    public static AutoLoginCheckResponseDTO valid(Long userId) {
        return AutoLoginCheckResponseDTO.builder()
                .canAutoLogin(true)
                .userId(userId)
                .message("存在有效的RememberMe token")
                .build();
    }

    /**
     * 创建无效token的响应
     */
    public static AutoLoginCheckResponseDTO invalid() {
        return AutoLoginCheckResponseDTO.builder()
                .canAutoLogin(false)
                .message("RememberMe token已过期")
                .build();
    }
}

