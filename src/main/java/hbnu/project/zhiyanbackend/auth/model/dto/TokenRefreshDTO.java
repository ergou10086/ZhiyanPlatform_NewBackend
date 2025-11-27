package hbnu.project.zhiyanbackend.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 令牌刷新响应体
 *
 * @author yxy
 */
@Data
public class TokenRefreshDTO {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
