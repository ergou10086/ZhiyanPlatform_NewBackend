package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户第三方账号绑定关系 DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户第三方账号绑定关系")
public class UserConnectionDTO {

    @Schema(description = "绑定关系ID")
    private String id;

    @Schema(description = "提供商名称", example = "github")
    private String provider;

    @Schema(description = "提供商用户ID")
    private String providerUserId;

    @Schema(description = "提供商用户名")
    private String providerUsername;

    @Schema(description = "提供商邮箱")
    private String providerEmail;

    @Schema(description = "提供商头像URL")
    private String providerAvatarUrl;

    @Schema(description = "绑定时间（时间戳）")
    private Long boundAt;

    @Schema(description = "最后同步时间（时间戳）")
    private Long lastSyncAt;
}