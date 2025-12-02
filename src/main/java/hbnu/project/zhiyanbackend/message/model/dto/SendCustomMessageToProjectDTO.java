package hbnu.project.zhiyanbackend.message.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送自定义消息给项目成员DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发送自定义消息给项目成员请求")
public class SendCustomMessageToProjectDTO {

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID", required = true, example = "123456")
    private Long projectId;

    @NotBlank(message = "消息标题不能为空")
    @Schema(description = "消息标题", required = true, example = "项目通知")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", required = true, example = "这是一条项目通知")
    private String content;
}