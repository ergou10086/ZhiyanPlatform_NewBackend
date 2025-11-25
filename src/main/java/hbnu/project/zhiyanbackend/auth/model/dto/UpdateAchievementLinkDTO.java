package hbnu.project.zhiyanbackend.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 更新成果关联信息请求DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新成果关联信息请求体")
public class UpdateAchievementLinkDTO {

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
