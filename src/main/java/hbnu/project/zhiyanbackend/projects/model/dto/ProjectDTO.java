package hbnu.project.zhiyanbackend.projects.model.dto;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目信息")
public class ProjectDTO {

    @Schema(description = "项目ID", example = "1977989681735929856")
    private String id;

    @Schema(description = "项目名称", example = "AI智能分析平台")
    private String name;

    @Schema(description = "项目描述", example = "基于深度学习的数据分析平台")
    private String description;

    @Schema(description = "项目状态", example = "IN_PROGRESS")
    private ProjectStatus status;

    @Schema(description = "项目可见性", example = "PUBLIC")
    private ProjectVisibility visibility;

    @Schema(description = "项目开始日期", example = "2025-01-01")
    private LocalDate startDate;

    @Schema(description = "项目结束日期", example = "2025-12-31")
    private LocalDate endDate;

    @Schema(description = "项目封面图片URL", example = "https://example.com/project.jpg")
    private String imageUrl;

    @Schema(description = "创建者ID", example = "1977989681735929856")
    private String creatorId;

    @Schema(description = "创建者名称", example = "张三")
    private String creatorName;

    @Schema(description = "项目成员数量", example = "10")
    private Integer memberCount;

    @Schema(description = "项目任务数量", example = "25")
    private Integer taskCount;

    @Schema(description = "创建时间", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间", example = "2025-10-31T15:30:00")
    private LocalDateTime updatedAt;
}

