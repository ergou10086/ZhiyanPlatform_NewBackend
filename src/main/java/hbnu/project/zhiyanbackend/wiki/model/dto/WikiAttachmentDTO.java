package hbnu.project.zhiyanbackend.wiki.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Wiki附件返回DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiAttachmentDTO {

    private String id;
    private String wikiPageId;
    private String projectId;
    private String attachmentType;

    private String fileName;
    private Long fileSize;
    private String fileSizeFormatted;
    private String fileType;
    private String mimeType;

    private String bucketName;
    private String objectKey;
    private String fileUrl;
    private String thumbnailUrl;

    private String description;
    private String fileHash;

    private String uploadBy;
    private LocalDateTime uploadAt;

    private Boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedBy;

    private Integer referenceCount;
    private Map<String, Object> metadata;
}
