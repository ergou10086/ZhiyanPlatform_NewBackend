package hbnu.project.zhiyanbackend.tasks.model.form;

//import hbnu.project.zhiyanbackend.tasks.model.enums.SubmissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提交任务请求体
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交任务请求")
public class SubmitTaskRequest {

    @NotBlank(message = "提交说明不能为空")
    @Size(min = 10, max = 5000, message = "提交说明长度必须在10-5000字符之间")
    @Schema(description = "提交说明（必填，描述任务完成情况）", required = true)
    private String submissionContent;

    @Schema(description = "附件URL列表（可选，用于提交成果文件、截图等）")
    private List<String> attachmentUrls;

    @Schema(description = "实际工时（单位：小时，可选)")
    private BigDecimal actualWorktime;
}
