package hbnu.project.zhiyanbackend.auth.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱修改数据传输对象
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailDTO {

    /**
     * 用户id
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 旧邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String oldEmail;

    /**
     * 新邮箱
     */
    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String newEmail;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度应为4-6位")
    private String verificationCode;

    /**
     * 2FA验证码（如果用户启用了2FA，此字段必填）
     */
    @Pattern(regexp = "^\\d{6}$", message = "2FA验证码必须是6位数字")
    private String twoFactorCode;
}