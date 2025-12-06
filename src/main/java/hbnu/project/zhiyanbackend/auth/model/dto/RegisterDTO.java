package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 用户注册DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户注册请求体")
public class RegisterDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    @Schema(description = "用户姓名", example = "张三", required = true)
    private String name;

    /**
     * 用户邮箱（登录账号）
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "用户邮箱（登录账号）", example = "user@example.com", required = true)
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度应为4-6位")
    @Schema(description = "邮箱验证码", example = "123456", required = true)
    private String verificationCode;

    /**
     * 用户密码
     * 规则：7-25位，必须包含至少一个字母，允许特殊符号
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 7, max = 25, message = "密码长度应为7-25位")
    @Pattern(regexp = ".*[a-zA-Z].*", message = "密码必须包含至少一个小写字母或大写字母")
    @Schema(description = "用户密码（7-25位，必须包含至少一个字母，允许特殊符号）", example = "password123", required = true)
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码（需与密码一致）", example = "password123", required = true)
    private String confirmPassword;

    /**
     * 用户职称/职位（可选）
     */
    @Size(max = 100, message = "职称/职位长度不能超过100个字符")
    @Schema(description = "用户职称/职位", example = "高级工程师")
    private String title;

    /**
     * 所属机构（可选）
     */
    @Size(max = 200, message = "所属机构长度不能超过200个字符")
    @Schema(description = "所属机构", example = "某某大学")
    private String institution;
}
