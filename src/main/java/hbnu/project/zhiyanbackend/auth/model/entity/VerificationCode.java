package hbnu.project.zhiyanbackend.auth.model.entity;

import hbnu.project.zhiyanbackend.auth.model.enums.VerificationCodeType;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtil;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 验证码实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "verification_codes", schema = "zhiyanauth",
        indexes = {
                @Index(name = "idx_verification_codes_email_type", columnList = "email, type"),
                @Index(name = "idx_verification_codes_expires_at", columnList = "expires_at"),
                @Index(name = "idx_verification_codes_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {

    /**
     * 雪花id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户邮箱
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * 验证码
     */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /**
     * 验证码类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private VerificationCodeType type;

    /**
     * 过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 是否已使用
     */
    @Builder.Default
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

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