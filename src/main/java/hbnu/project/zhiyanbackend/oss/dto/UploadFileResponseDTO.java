package hbnu.project.zhiyanbackend.oss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 文件上传结果响应对象
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@Schema(description = "COS 文件上传响应")
public class UploadFileResponseDTO {

    @Schema(description = "对象键", example = "uploads/2025/11/26/xxx.png")
    private String objectKey;

    @Schema(description = "公网访问地址")
    private String url;

    @Schema(description = "腾讯云返回的 eTag")
    private String eTag;

    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "文件大小（字节）")
    private long size;

    @Schema(description = "文件类型")
    private String contentType;
}


