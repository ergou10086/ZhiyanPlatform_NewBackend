package hbnu.project.zhiyanbackend.projects.model.form;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新成员角色请求体
 * 该类用于封装更新成员角色所需的请求数据

 *
 * @author Tokito 创建该类的作者
 */
@Data // Lombok注解，自动生成getter、setter等方法
@Builder // Lombok注解，提供Builder模式构建对象
@NoArgsConstructor // Lombok注解，生成无参构造方法
@AllArgsConstructor // Lombok注解，生成包含所有参数的构造方法
@Schema(description = "更新成员角色请求") // Swagger注解，用于API文档生成
public class UpdateMemberRoleRequest {

    @NotNull(message = "新角色不能为空") // Bean Validation注解，表示该字段不能为空
    @Schema(description = "新角色", required = true) // Swagger注解，描述字段信息，标记为必需
    private ProjectMemberRole newRole; // 成员的新角色，类型为ProjectMemberRole枚举
}

