package hbnu.project.zhiyanbackend.projects.model.dto;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目成员详细信息DTO
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目成员详细信息")
public class ProjectMemberDetailDTO {

    @LongToString
    @Schema(description = "成员ID")
    private Long id;

    @LongToString
    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @LongToString
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "用户头像URL或Base64数据")
    // TODO COS_AVATAR_MIGRATE: 标准化为 COS 头像 URL，逐步移除 Base64 形式
    private String avatar;

    @Schema(description = "项目角色")
    private ProjectMemberRole projectRole;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "加入时间")
    private LocalDateTime joinedAt;

    @Schema(description = "是否为当前用户")
    private Boolean isCurrentUser;
}

