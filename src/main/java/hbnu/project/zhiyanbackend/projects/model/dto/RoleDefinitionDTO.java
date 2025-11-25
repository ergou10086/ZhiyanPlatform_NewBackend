package hbnu.project.zhiyanbackend.projects.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 项目角色定义 DTO（精简版）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目角色定义信息")
public class RoleDefinitionDTO {

    @Schema(description = "角色代码", example = "OWNER")
    private String code;

    @Schema(description = "角色名称", example = "项目拥有者")
    private String name;

    @Schema(description = "角色说明")
    private String description;

    @Schema(description = "拥有的权限代码列表")
    private List<String> permissions;
}
