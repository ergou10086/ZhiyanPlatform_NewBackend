package hbnu.project.zhiyanbackend.tasks.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务提交附件上传/下载响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionFileResponse {

    @Schema(description = "可用于提交记录的文件URL（相对路径）", example = "2025/12/01/uuid-file.txt")
    private String url;

    @Schema(description = "原始文件名", example = "需求说明.docx")
    private String filename;

    @Schema(description = "文件 MIME 类型", example = "application/pdf")
    private String contentType;

    @Schema(description = "文件大小（字节）", example = "1048576")
    private long size;
}


