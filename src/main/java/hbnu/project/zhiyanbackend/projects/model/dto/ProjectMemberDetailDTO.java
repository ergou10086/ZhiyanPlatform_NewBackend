package hbnu.project.zhiyanbackend.projects.model.dto;

import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目成员详细信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDetailDTO {

    private Long id;

    private Long projectId;

    private String projectName;

    private Long userId;

    private String username;

    private String email;

    private ProjectMemberRole projectRole;

    private String roleName;

    private LocalDateTime joinedAt;

    private Boolean isCurrentUser;
}

