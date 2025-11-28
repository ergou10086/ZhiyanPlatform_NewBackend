package hbnu.project.zhiyanbackend.projects.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图片上传响应")
public class ImageUploadResponse {

    @Schema(description = "图片访问URL", example = "http://localhost:9000/project-covers/2025/01/123456.jpg")
    private String imageUrl;

    @Schema(description = "原始文件名", example = "project-cover.jpg")
    private String originalFilename;

    @Schema(description = "文件大小（字节）", example = "102400")
    private Long fileSize;

    @Schema(description = "文件ETag", example = "5d41402abc4b2a76b9719d911017c592")
    private String eTag;
}

