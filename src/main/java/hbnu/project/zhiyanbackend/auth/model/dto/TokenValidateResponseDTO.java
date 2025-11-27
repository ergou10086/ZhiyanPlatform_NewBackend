package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.*;

/**
 * Token验证响应DTO
 * 用于返回Token验证的详细结果
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidateResponseDTO {

    /**
     * 令牌是否有效
     */
    private Boolean valid;

    /**
     * 用户ID（字符串格式）
     */
    private String userId;

    /**
     * 用户角色（逗号分隔）
     */
    private String roles;

    /**
     * 令牌剩余有效时间（秒）
     */
    private Long remainingTime;

    /**
     * 验证消息/错误信息
     */
    private String message;
}

