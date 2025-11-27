package hbnu.project.zhiyanbackend.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
     * 当前密码
     */
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    /**
     * 旧邮箱
     */
    @NotBlank(message = "旧邮箱不能为空")
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
}