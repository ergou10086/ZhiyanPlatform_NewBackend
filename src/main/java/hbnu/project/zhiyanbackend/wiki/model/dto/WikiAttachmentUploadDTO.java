// Reconstitution/zhiyan-backend/src/main/java/hbnu/project/zhiyanbackend/wiki/model/dto/WikiAttachmentUploadDTO.java
package hbnu.project.zhiyanbackend.wiki.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wiki附件上传DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiAttachmentUploadDTO {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotNull(message = "Wiki页面ID不能为空")
    private Long wikiPageId;

    @NotNull(message = "上传者不能为空")
    private Long uploadBy;

    /**
     * 可选：附件类型（IMAGE/FILE），空则自动判定
     */
    private String attachmentType;

    /**
     * 备注
     */
    private String description;

    /**
     * 自定义文件名（传给 COS，允许为空）
     */
    private String customFileName;
}
