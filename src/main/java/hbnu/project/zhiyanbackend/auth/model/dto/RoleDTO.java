package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 角色数据传输对象
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 是否为系统默认角色
     */
    private Boolean isSystemDefault;

    /**
     * 权限名称集合
     */
    private Set<String> permissions;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 最后修改人ID
     */
    private Long updatedBy;
}

