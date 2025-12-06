package hbnu.project.zhiyanbackend.auth.model.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 忘记密码DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "忘记密码请求体")
public class ForgetPasswordDTO {

    /**
     * 用户邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "用户邮箱", example = "user@example.com", required = true)
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度应为4-6位")
    @Schema(description = "邮箱验证码", example = "123456", required = true)
    private String verificationCode;

    /**
     * 新密码
     * 规则：7-25位，必须包含至少一个字母，允许特殊符号
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 7, max = 25, message = "密码长度应为7-25位")
    @Pattern(regexp = ".*[a-zA-Z].*", message = "密码必须包含至少一个小写字母或大写字母")
    @Schema(description = "新密码（7-25位，必须包含至少一个字母，允许特殊符号）", example = "newpassword123", required = true)
    private String newPassword;
}
