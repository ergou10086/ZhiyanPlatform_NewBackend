package hbnu.project.zhiyanbackend.auth.model.entity;

import hbnu.project.zhiyanbackend.auth.model.enums.QRCodeStatus;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;

import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 扫码登录实体类
 * 用于存储二维码登录相关信息
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "qr_code_logins", schema = "zhiyanauth",
        indexes = {
                @Index(name = "idx_qr_code", columnList = "qr_code"),
                @Index(name = "idx_expire_time", columnList = "expire_time")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeLogin {

    /**
     * 主键ID
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 二维码唯一标识
     * PC端显示,移动端扫描
     */
    @Column(name = "qr_code", nullable = false, unique = true, length = 128)
    private String qrCode;

    /**
     * 二维码状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private QRCodeStatus status = QRCodeStatus.PENDING;

    /**
     * 扫描后记录用户ID
     */
    @LongToString
    @Column(name =  "scan_user_id", length = 64)
    private Long scanUserId;

    /**
     * 扫描时间
     */
    @Column(name = "scan_time")
    private LocalDateTime scanTime;

    /**
     * 确认时间
     */
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    /**
     * 生成的Access Token
     * 确认后生成,PC端获取
     */
    @Column(name = "access_token", length = 1000)
    private String accessToken;

    /**
     * 生成的Refresh Token
     */
    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    /**
     * Token过期时间(秒)
     */
    @Column(name = "token_expires_in")
    private Long tokenExpiresIn;

    /**
     * 二维码过期时间
     * 默认5分钟
     */
    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    /**
     * 客户端IP地址
     * PC端生成二维码时的IP
     */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /**
     * 在持久化之前生成雪花ID
     * 设置默认的过期时间为当前时间+5min
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }

        if (this.expireTime == null) {
            this.expireTime = LocalDateTime.now().plusMinutes(5);
        }
    }

    /**
     * 检查二维码是否已过期
     */
    @Transient
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expireTime);
    }

    /**
     * 检查二维码是否可以被扫描
     */
    @Transient
    public boolean canBeScanned() {
        return this.status == QRCodeStatus.PENDING && !isExpired();
    }

    /**
     * 检查二维码是否可以被确认
     */
    @Transient
    public boolean canBeConfirmed() {
        return this.status == QRCodeStatus.SCANNED && !isExpired();
    }
}
