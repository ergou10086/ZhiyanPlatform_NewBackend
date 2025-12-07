package hbnu.project.zhiyanbackend.projects.model.form;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import hbnu.project.zhiyanbackend.security.xss.Xss;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 更新项目请求体
 * 该类用于封装更新项目所需的请求数据
 *
 * @author Tokito
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新项目请求")
public class UpdateProjectRequest {

    @Xss(message = "项目名称包含非法字符")
    @Schema(description = "项目名称")
    private String name;  // 项目名称，用于标识项目的唯一名称

    @Xss(message = "项目描述包含非法字符")
    @Schema(description = "项目描述")
    private String description;  // 项目的详细描述信息

    @Schema(description = "可见性（PUBLIC/PRIVATE）")
    private ProjectVisibility visibility;  // 项目的可见性设置，公开或私有

    @Schema(description = "项目状态")
    private ProjectStatus status;  // 当前的项目状态，如进行中、已完成等

    @Xss(message = "项目图片URL包含非法字符")
    @Schema(description = "项目图片URL")
    private String imageUrl;  // 项目相关图片的访问链接

    @Schema(description = "开始日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;  // 项目开始的日期，格式为ISO标准日期格式

    @Schema(description = "结束日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;  // 项目预计结束的日期，格式为ISO标准日期格式
}

