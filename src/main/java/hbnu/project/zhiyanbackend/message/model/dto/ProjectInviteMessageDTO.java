package hbnu.project.zhiyanbackend.message.model.dto;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目成员邀请请求")
public class ProjectInviteMessageDTO {

    @NotNull
    @Schema(description = "项目ID", required = true)
    private Long projectId;

    @NotNull
    @Schema(description = "被邀请用户ID", required = true)
    private Long targetUserId;

    @NotNull
    @Schema(description = "邀请时希望赋予的角色", required = true)
    private ProjectMemberRole role;

    @Schema(description = "附加邀请说明")
    private String message;
}