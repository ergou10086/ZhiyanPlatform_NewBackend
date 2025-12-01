package hbnu.project.zhiyanbackend.wiki.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Wiki统计信息DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiStatisticsDTO {

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 总页面数
     */
    private Long totalPages;

    /**
     * 文档数量
     */
    private Long documentCount;

    /**
     * 目录数量
     */
    private Long directoryCount;

    /**
     * 总内容大小（字符数）
     */
    private Long totalContentSize;

    /**
     * 贡献者数量
     */
    private Integer contributorCount;

    /**
     * 最近30天更新数
     */
    private Long recentUpdates;

    /**
     * 总版本数
     */
    private Long totalVersions;

    /**
     * 贡献者统计（用户ID -> 编辑次数）
     */
    private Map<String, Integer> contributorStats;
}