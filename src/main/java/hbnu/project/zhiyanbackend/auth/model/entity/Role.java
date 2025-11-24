package hbnu.project.zhiyanbackend.auth.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 角色实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "roles", schema = "zhiyanauth")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseAuditEntity {

    /**
     * 雪花id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 角色名称
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * 角色描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 角色类型 - 使用字符串存储（系统角色固定为SYSTEM）
     */
    @Column(name = "role_type", nullable = false, length = 20)
    private String roleType;

    /**
     * 是否为系统默认角色
     */
    @Column(name = "is_system_default", nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    /**
     * 角色用户关联（一对多）
     */
    @JsonIgnore
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserRole> userRoles;

    /**
     * 角色权限关联（一对多）
     */
    @JsonIgnore
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RolePermission> rolePermissions;


    /**
     * 在持久化之前生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
    }
}
