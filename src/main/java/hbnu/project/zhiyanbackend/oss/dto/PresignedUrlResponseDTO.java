package hbnu.project.zhiyanbackend.oss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 预签名URL响应对象
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@Schema(description = "预签名URL响应")
public class PresignedUrlResponseDTO {

    @Schema(description = "预签名URL", example = "https://bucket.cos.ap-beijing.myqcloud.com/...")
    private String url;

    @Schema(description = "对象键", example = "uploads/2025/11/26/xxx.png")
    private String objectKey;

    @Schema(description = "HTTP方法", example = "GET")
    private String method;

    @Schema(description = "过期分钟数", example = "60")
    private Integer expireMinutes;

    @Schema(description = "文件类型")
    private String contentType;

    @Schema(description = "下载时的文件名")
    private String downloadFilename;
}