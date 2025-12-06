package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2补充信息创建账号请求体
 * 用于补充必要信息（邮箱、密码）创建新账号
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2补充信息创建账号请求体")
public class OAuth2SupplementInfoDTO {

    /**
     * OAuth2提供商名称
     */
    @NotBlank(message = "提供商名称不能为空")
    @Schema(description = "OAuth2提供商名称", example = "github", required = true)
    private String provider;

    /**
     * OAuth2用户ID（提供商中的用户ID）
     */
    @NotBlank(message = "OAuth2用户ID不能为空")
    @Schema(description = "OAuth2用户ID", required = true)
    private String providerUserId;

    /**
     * 邮箱（必填）
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "user@example.com", required = true)
    private String email;

    /**
     * 密码
     * 规则：7-25位，必须包含至少一个字母，允许特殊符号
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 7, max = 25, message = "密码长度必须在7-25位之间")
    @Pattern(regexp = ".*[a-zA-Z].*", message = "密码必须包含至少一个小写字母或大写字母")
    @Schema(description = "密码（7-25位，必须包含至少一个字母，允许特殊符号）", example = "password123", required = true)
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "password123", required = true)
    private String confirmPassword;

    /**
     * OAuth2用户信息（可选，用于更新头像、昵称等）
     */
    @Schema(description = "OAuth2用户信息（可选）")
    private OAuth2UserInfoDTO oauth2UserInfo;
}

