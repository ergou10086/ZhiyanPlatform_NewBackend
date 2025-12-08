package hbnu.project.zhiyanbackend.projects.model.form;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import hbnu.project.zhiyanbackend.security.xss.Xss;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 保存草稿请求体
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "保存项目草稿请求")
public class SaveDraftRequest {

    @Xss(message = "项目名称包含非法字符")
    @Schema(description = "项目名称", example = "AI智能分析平台")
    private String name;

    @Xss(message = "项目描述包含非法字符")
    @Schema(description = "项目描述", example = "基于深度学习的数据分析平台")
    private String description;

    @Schema(description = "可见性（PUBLIC/PRIVATE）", example = "PRIVATE")
    private ProjectVisibility visibility;

    @Schema(description = "开始日期", example = "2025-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2025-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Schema(description = "项目图片URL", example = "https://example.com/project-image.jpg")
    private String imageUrl;

    @Schema(description = "项目标签列表", example = "[\"AI\", \"数据分析\"]")
    private List<String> tags;
}

