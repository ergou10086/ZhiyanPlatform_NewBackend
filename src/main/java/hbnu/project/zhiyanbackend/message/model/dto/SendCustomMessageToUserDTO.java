package hbnu.project.zhiyanbackend.message.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送自定义消息给指定用户DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发送自定义消息给指定用户请求")
public class SendCustomMessageToUserDTO {

    @NotBlank(message = "接收者用户名不能为空")
    @Schema(description = "接收者用户名", required = true, example = "张三")
    private String receiverUsername;

    @NotBlank(message = "消息标题不能为空")
    @Schema(description = "消息标题", required = true, example = "重要通知")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", required = true, example = "这是一条重要消息")
    private String content;
}