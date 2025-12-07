package hbnu.project.zhiyanbackend.projects.model.form;

import hbnu.project.zhiyanbackend.security.xss.Xss;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新角色请求
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新角色请求")
public class UpdateRoleRequest {

    @NotBlank(message = "角色代码不能为空")
    @Xss(message = "角色代码包含非法字符")
    @Schema(description = "角色代码 (OWNER/MEMBER)", required = true, example = "MEMBER")
    private String roleCode;
}

