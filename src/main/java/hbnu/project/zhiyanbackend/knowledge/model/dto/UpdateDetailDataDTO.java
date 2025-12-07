package hbnu.project.zhiyanbackend.knowledge.model.dto;

import hbnu.project.zhiyanbackend.security.xss.Xss;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * 更新成果详情数据DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDetailDataDTO {

    /**
     * 成果ID
     */
    @NotNull(message = "成果ID不能为空")
    private Long achievementId;

    /**
     * 详细信息（自定义字段的键值对）
     */
    private Map<String, Object> detailData;

    /**
     * 摘要/描述
     */
    @Xss(message = "摘要包含非法字符")
    private String abstractText;
}
