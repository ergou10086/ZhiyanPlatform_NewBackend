package hbnu.project.zhiyanbackend.projects.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.dto.PermissionDefinitionDTO;
import hbnu.project.zhiyanbackend.projects.model.dto.RoleDefinitionDTO;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目角色控制器（精简版）
 * 仅提供角色/权限定义查询，不做权限校验，不依赖外部服务。
 */
@Slf4j
@RestController
@RequestMapping("/api/projects")
@Tag(name = "项目角色管理(精简版)", description = "项目角色、权限定义查询接口")
public class ProjectRoleController {

    /**
     * 获取所有项目角色定义（从枚举中构造）
     */
    @GetMapping("/role-definitions")
    @Operation(summary = "获取项目角色定义列表", description = "返回所有项目角色的定义信息")
    public R<List<RoleDefinitionDTO>> getAllRoleDefinitions() {
        List<RoleDefinitionDTO> roles = Arrays.stream(ProjectMemberRole.values())
                .map(role -> RoleDefinitionDTO.builder()
                        .code(role.name())
                        .name(role.getRoleName())
                        .description(role.getDescription())
                        .permissions(role.getPermissions().stream()
                                .map(ProjectPermission::getCode)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return R.ok(roles);
    }

    /**
     * 获取指定角色的详细定义
     */
    @GetMapping("/role-definitions/{roleCode}")
    @Operation(summary = "获取角色定义详情", description = "根据角色代码获取角色定义信息")
    public R<RoleDefinitionDTO> getRoleDefinition(
            @PathVariable("roleCode") @Parameter(description = "角色代码", example = "OWNER") String roleCode
    ) {
        try {
            ProjectMemberRole role = ProjectMemberRole.valueOf(roleCode);
            RoleDefinitionDTO dto = RoleDefinitionDTO.builder()
                    .code(role.name())
                    .name(role.getRoleName())
                    .description(role.getDescription())
                    .permissions(role.getPermissions().stream()
                            .map(ProjectPermission::getCode)
                            .collect(Collectors.toList()))
                    .build();
            return R.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("角色代码不存在: {}", roleCode);
            return R.fail("角色不存在: " + roleCode);
        }
    }

    /**
     * 获取所有项目权限定义
     */
    @GetMapping("/permission-definitions")
    @Operation(summary = "获取项目权限定义列表", description = "返回所有项目权限的定义信息")
    public R<List<PermissionDefinitionDTO>> getAllPermissionDefinitions() {
        List<PermissionDefinitionDTO> permissions = Arrays.stream(ProjectPermission.values())
                .map(perm -> PermissionDefinitionDTO.builder()
                        .code(perm.getCode())
                        .description(perm.getDescription())
                        .build())
                .collect(Collectors.toList());

        return R.ok(permissions);
    }
}
