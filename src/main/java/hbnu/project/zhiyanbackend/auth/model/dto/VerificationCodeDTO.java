package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 验证码请求DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码请求体")
public class VerificationCodeDTO {

    /**
     * 用户邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "用户邮箱", example = "user@example.com", required = true)
    private String email;

    /**
     * 验证码类型
     */
    @NotBlank(message = "验证码类型不能为空")
    @Schema(description = "验证码类型（REGISTER/RESET_PASSWORD/CHANGE_EMAIL）", example = "REGISTER", required = true)
    private String type;
}

