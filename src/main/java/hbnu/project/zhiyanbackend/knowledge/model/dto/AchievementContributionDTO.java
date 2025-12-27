package hbnu.project.zhiyanbackend.knowledge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 成果贡献统计DTO
 * 用于返回贡献热力图所需的数据
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementContributionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 该日期的贡献数
     */
    private Long count;
}

