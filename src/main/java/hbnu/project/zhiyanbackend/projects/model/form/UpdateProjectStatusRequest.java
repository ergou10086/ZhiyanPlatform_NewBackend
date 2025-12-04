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
 * 该类用于封装更新项目状态时所需的请求数据
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新项目状态请求")  // Swagger注解，用于API文档生成
public class UpdateProjectStatusRequest {

    @NotNull(message = "状态不能为空")  // Bean Validation注解，表示该字段不能为null
    @Schema(description = "项目状态", required = true)  // Swagger注解，描述字段信息
    private ProjectStatus status;  // 项目状态属性，使用ProjectStatus枚举类型
}

