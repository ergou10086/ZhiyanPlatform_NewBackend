package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 关联学术成果请求DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关联学术成果请求体")
public class AchievementLinkDTO {

    /**
     * 成果ID
     */
    @NotBlank(message = "成果ID不能为空")
    @Schema(description = "成果ID", example = "123456", required = true)
    private String achievementId;

    /**
     * 项目ID（可选）
     */
    @Schema(description = "项目ID", example = "789012")
    private String projectId;

    /**
     * 展示顺序
     */
    @Schema(description = "展示顺序", example = "1")
    private Integer displayOrder;

    /**
     * 备注说明
     */
    @Schema(description = "备注说明", example = "重要成果")
    private String remark;
}
