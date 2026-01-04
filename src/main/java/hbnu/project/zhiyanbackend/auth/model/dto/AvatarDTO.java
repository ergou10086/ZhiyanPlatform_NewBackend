package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 用户头像DTO
 * 使用PostgreSQL BYTEA存储，返回Base64编码的图片数据
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户头像信息")
public class AvatarDTO {

    @Schema(description = "头像访问URL（优先使用 COS URL）")
    private String avatarUrl;

    @Schema(description = "头像MIME类型（如：image/jpeg, image/png）")
    private String contentType;

    @Schema(description = "头像文件大小（字节）")
    private Long size;
}
