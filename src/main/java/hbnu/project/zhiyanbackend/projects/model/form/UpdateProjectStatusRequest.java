package hbnu.project.zhiyanbackend.projects.model.form;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新项目状态请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新项目状态请求")
public class UpdateProjectStatusRequest {

    @NotNull(message = "状态不能为空")
    @Schema(description = "项目状态", required = true)
    private ProjectStatus status;
}

