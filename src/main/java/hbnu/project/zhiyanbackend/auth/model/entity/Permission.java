package hbnu.project.zhiyanbackend.auth.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

/**
 * 权限实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "permissions", schema = "zhiyanauth")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Permission extends BaseAuditEntity {

    /**
     * 雪花id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 权限名称
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 权限描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 角色权限关联（一对多）
     */
    @JsonIgnore
    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RolePermission> rolePermissions;

    /**
     * 在持久化之前生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtil.nextId();
        }
    }
}

