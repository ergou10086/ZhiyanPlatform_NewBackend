package hbnu.project.zhiyanbackend.projects.model.form;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 创建项目请求体
 * 该类用于封装创建项目所需的请求数据，使用了Lombok注解简化代码
 * 包含了项目的基本信息，如名称、描述、可见性等
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建项目请求")


    // 项目名称字段，必填项
public class CreateProjectRequest {  // Bean Validation注解，确保字段不为空
  // Swagger注解
    @NotBlank(message = "项目名称不能为空")
    @Schema(description = "项目名称", required = true, example = "AI智能分析平台")


    // 项目描述字段，选填项
    private String name;  // Swagger注解

    @Schema(description = "项目描述", example = "基于深度学习的数据分析平台")


    // 项目可见性字段，默认为PUBLIC
    private String description;  // Swagger注解

    @Schema(description = "可见性（PUBLIC/PRIVATE），默认PUBLIC", example = "PUBLIC")


    // 项目图片URL字段，必填项
    private ProjectVisibility visibility;  // Bean Validation注解，确保字段不为空
  // Swagger注解
    @NotBlank(message = "项目图片URL不能为空")
    @Schema(description = "项目图片URL", required = true, example = "https://example.com/project-image.jpg")


    // 项目开始日期字段
    private String imageUrl;  // Swagger注解
  // Spring注解，用于日期格式化
    @Schema(description = "开始日期", example = "2025-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)


    // 项目结束日期字段
    private LocalDate startDate;  // Swagger注解
  // Spring注解，用于日期格式化
    @Schema(description = "结束日期", example = "2025-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}

