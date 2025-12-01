package hbnu.project.zhiyanbackend.wiki.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wiki搜索结果DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiSearchDTO {

    /**
     * 页面ID
     */
    private Long id;

    /**
     * 项目ID（新增字段）
     */
    private Long projectId;

    /**
     * 页面标题
     */
    private String title;

    /**
     * 页面路径
     */
    private String path;

    /**
     * 页面类型
     */
    private String pageType;

    /**
     * 内容摘要（高亮匹配部分）
     */
    private String contentSummary;

    /**
     * 内容上下文（新增字段，用于存储匹配的上下文）
     */
    private String contentContext;

    /**
     * 搜索得分（新增字段，用于排序）
     */
    private Float score;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 最后编辑者ID（修正为Long类型，与实体类保持一致）
     */
    private Long lastEditorId;
}