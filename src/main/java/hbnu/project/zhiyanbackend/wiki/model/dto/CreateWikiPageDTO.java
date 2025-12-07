package hbnu.project.zhiyanbackend.wiki.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hbnu.project.zhiyanbackend.security.xss.Xss;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建Wiki页面DTO
 * 用于接收前端创建Wiki页面的请求数据
 *
 * @author ErgouTree
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateWikiPageDTO {

    /**
     * 所属项目ID（必填）
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 页面标题（必填，1-255字符）
     */
    @NotBlank(message = "页面标题不能为空")
    @Size(min = 1, max = 255, message = "页面标题长度必须在1-255字符之间")
    @Xss(message = "页面标题包含非法字符")
    private String title;

    /**
     * 页面类型（目录DIRECTORY或文档DOCUMENT，默认文档）
     */
    private PageType pageType = PageType.DOCUMENT;

    /**
     * Markdown内容（可选，仅当pageType为DOCUMENT时有效）
     * 注意：Markdown内容可能包含HTML标签，但为了安全，仍然进行XSS检查
     * 如果后续需要支持富文本，可以考虑放宽此限制
     */
    private String content;

    /**
     * 父页面ID（可选，不填则为根页面）
     */
    private Long parentId;

    /**
     * 排序序号（可选，不填则自动排到最后）
     */
    private Integer sortOrder;

    /**
     * 是否公开（默认false）
     */
    private Boolean isPublic = false;

    /**
     * 修改说明（可选，用于版本历史）
     */
    @Size(max = 500, message = "修改说明长度不能超过500字符")
    @Xss(message = "修改说明包含非法字符")
    private String changeDescription;

    /**
     * 创建者ID（由后端从上下文获取）
     */
    private Long creatorId;
}
