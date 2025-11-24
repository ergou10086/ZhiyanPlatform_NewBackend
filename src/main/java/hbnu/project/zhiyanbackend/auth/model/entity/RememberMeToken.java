package hbnu.project.zhiyanbackend.auth.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Remember Me Token实体类
 * 用于存储用户的"记住我"功能token
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "remember_me_tokens", schema = "zhiyanauth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RememberMeToken {
    
    /**
     * 雪花id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户id
     */
    @LongToString
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * remember me token
     */
    @Column(name = "token", unique = true, nullable = false, length = 128)
    private String token;

    /**
     * remember me token的过期时间
     */
    @Column(name = "expiry_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiryTime;

    /**
     * remember me token的创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 在持久化之前生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtil.nextId();
        }
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
    }
}

