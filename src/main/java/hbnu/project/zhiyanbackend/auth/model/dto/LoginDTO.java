package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 用户登录DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录请求体")
public class LoginDTO {
    
    /**
     * 登录邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "登录邮箱", example = "user@example.com", required = true)
    private String email;

    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "用户密码", example = "password123", required = true)
    private String password;

    /**
     * "记住我"选项
     */
    @Schema(description = "是否记住我", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean rememberMe = false;

    /**
     * 验证码（可选）
     */
    @Schema(description = "验证码（可选，用于二次验证）", example = "123456")
    private String verificationCode;
}

