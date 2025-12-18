package hbnu.project.zhiyanbackend.knowledge.model.dto;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 成果文件DTO
 * 用于返回文件信息
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成果文件信息")
public class AchievementFileDTO {

    /**
     * 文件ID
     */
    @LongToString
    @Schema(description = "文件ID")
    private Long id;

    /**
     * 所属成果ID
     */
    @LongToString
    @Schema(description = "所属成果ID")
    private Long achievementId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（扩展名）
     */
    private String fileType;

    /**
     * 文件访问URL（COS公网访问地址）
     */
    private String fileUrl;

    /**
     * 上传者ID
     */
    @LongToString
    @Schema(description = "上传者ID")
    private Long uploadBy;

    /**
     * 上传时间
     */
    private LocalDateTime uploadAt;
}
