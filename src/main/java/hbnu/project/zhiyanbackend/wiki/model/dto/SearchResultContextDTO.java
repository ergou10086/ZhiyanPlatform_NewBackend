package hbnu.project.zhiyanbackend.wiki.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索结果上下文DTO
 * 用于存储关键字匹配的具体位置和上下文
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultContextDTO {

    /**
     * 匹配的片段内容
     */
    private String snippet;

    /**
     * 关键字在内容中的起始位置
     */
    private Integer startPos;

    /**
     * 关键字在内容中的结束位置
     */
    private Integer endPos;
}
