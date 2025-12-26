package hbnu.project.zhiyanbackend.projects.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个项目所有权移交请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目所有权移交请求")
public class ProjectOwnershipTransferRequest {

    @Schema(description = "项目ID", example = "1")
    private Long projectId;

    @Schema(description = "新的项目拥有者用户ID", required = true, example = "1001")
    private Long newOwnerId;
}
