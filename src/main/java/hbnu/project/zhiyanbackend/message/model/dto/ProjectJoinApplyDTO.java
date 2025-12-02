package hbnu.project.zhiyanbackend.message.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目加入申请")
public class ProjectJoinApplyDTO {

    @NotNull
    @Schema(description = "申请加入的项目ID", required = true)
    private Long projectId;

    @Schema(description = "申请说明")
    private String reason;
}