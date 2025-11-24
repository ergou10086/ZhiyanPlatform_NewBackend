package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.*;

/**
 * Token验证响应DTO
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
     * 是否有效
     */
    private Boolean valid;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 错误信息
     */
    private String error;
}

