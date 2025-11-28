package hbnu.project.zhiyanbackend.projects.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目权限定义 DTO（精简版）
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目权限定义信息")
public class PermissionDefinitionDTO {

    @Schema(description = "权限代码", example = "project:manage")
    private String code;

    @Schema(description = "权限说明")
    private String description;
}
