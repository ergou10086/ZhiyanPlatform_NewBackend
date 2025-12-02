package hbnu.project.zhiyanbackend.tasks.model.dto;

import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务提交记录DTO
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务提交记录信息")
public class TaskSubmissionDTO {

    @Schema(description = "提交记录ID")
    private String id;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务标题")
    private String taskTitle;

    @Schema(description = "任务创建者ID")
    private String taskCreatorId;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "提交人ID")
    private String submitterId;

    @Schema(description = "提交人信息")
    private UserDTO submitter;

    @Schema(description = "提交说明")
    private String submissionContent;

    @Schema(description = "附件URL列表")
    private List<String> attachmentUrls;

    @Schema(description = "提交时间")
    private LocalDateTime submissionTime;

    @Schema(description = "审核状态")
    private ReviewStatus reviewStatus;

    @Schema(description = "审核人ID")
    private String reviewerId;

    @Schema(description = "审核人信息")
    private UserDTO reviewer;

    @Schema(description = "审核意见")
    private String reviewComment;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "实际工时（小时）")
    private BigDecimal actualWorktime;

    @Schema(description = "提交版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
