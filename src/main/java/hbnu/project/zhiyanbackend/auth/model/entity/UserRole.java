package hbnu.project.zhiyanbackend.auth.model.entity;

import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtil;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 * 用于维护用户(User)与角色(Role)之间的多对多关联关系
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "user_roles", schema = "zhiyanauth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    /**
     * 雪花id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 关联的用户对象
     * 多对一关系：多个用户角色关联记录可以对应同一个用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_USER_ROLE_USER"))
    private User user;

    /**
     * 关联的角色对象
     * 多对一关系：多个用户角色关联记录可以对应同一个角色
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_USER_ROLE_ROLE"))
    private Role role;

    /**
     * 角色分配时间
     * 记录该角色被分配给用户的具体时间
     */
    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime assignedAt;

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
