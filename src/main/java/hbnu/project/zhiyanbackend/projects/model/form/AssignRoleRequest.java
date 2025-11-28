package hbnu.project.zhiyanbackend.projects.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配角色请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分配角色请求")
public class AssignRoleRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true)
    private Long userId;

    @NotBlank(message = "角色代码不能为空")
    @Schema(description = "角色代码 (OWNER/MEMBER)", required = true, example = "MEMBER")
    private String roleCode;
}

