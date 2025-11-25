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

    @Schema(description = "头像Base64编码数据（data:image/jpeg;base64,...格式）")
    private String avatarData;

    @Schema(description = "头像MIME类型（如：image/jpeg, image/png）")
    private String contentType;

    @Schema(description = "头像文件大小（字节）")
    private Long size;

    /**
     * 获取完整的Data URL格式
     * 格式：data:image/jpeg;base64,/9j/4AAQSkZJRg...
     *
     * @return Data URL
     */
    public String getDataUrl() {
        if (avatarData == null || contentType == null) {
            return null;
        }
        // 如果已经是data URL格式，直接返回
        if (avatarData.startsWith("data:")) {
            return avatarData;
        }
        // 否则拼接成data URL格式
        return "data:" + contentType + ";base64," + avatarData;
    }
}
