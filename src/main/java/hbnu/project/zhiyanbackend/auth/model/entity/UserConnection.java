package hbnu.project.zhiyanbackend.auth.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 用户第三方账号绑定关系表
 * 采用"主账号 + 绑定关系"模式，存储用户与第三方平台的关联
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "user_connections", schema = "zhiyanauth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider_user",
                        columnNames = {"provider", "provider_user_id"})
        },
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_provider_user_id", columnList = "provider, provider_user_id")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserConnection extends BaseAuditEntity {

    /**
     * 主键ID（雪花ID）
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 平台用户ID（关联到 User 表）
     */
    @LongToString
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * OAuth2 提供商名称
     * 例如：github, orcid, google, wechat
     */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * 第三方平台的用户唯一标识（OpenID / sub）
     * GitHub: user ID
     * ORCID: ORCID iD (例如: 0000-0002-1825-0097)
     * Google: sub
     */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    /**
     * 第三方平台的用户名（用于显示）
     * GitHub: login
     * ORCID: name
     */
    @Column(name = "provider_username", length = 255)
    private String providerUsername;

    /**
     * 第三方平台返回的邮箱
     */
    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    /**
     * 第三方平台的头像URL
     */
    @Column(name = "provider_avatar_url", length = 500)
    private String providerAvatarUrl;

    /**
     * 访问令牌（Access Token）
     * 某些场景需要保存以便后续调用第三方API
     * 例如：ORCID 的 token 有效期 20 年，可以长期保存
     */
    @Column(name = "access_token", length = 1000)
    private String accessToken;

    /**
     * 刷新令牌（Refresh Token）
     * 某些提供商支持刷新令牌
     */
    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    /**
     * 令牌过期时间（时间戳，毫秒）
     */
    @Column(name = "token_expires_at")
    private Long tokenExpiresAt;

    /**
     * 绑定时的额外信息（JSON格式）
     * 可以存储一些提供商特有的信息
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    /**
     * 是否已解绑
     */
    @Builder.Default
    @Column(name = "is_unbound", nullable = false)
    private Boolean isUnbound = false;

    /**
     * ORCID访问令牌
     * 用于调用ORCID API获取用户详细信息，通常有效期为20年
     * 注意：这个字段主要用于ORCID，其他Provider可以不使用
     */
    @Column(name = "orcid_access_token", length = 500)
    @JsonIgnore
    private String orcidAccessToken;

    /**
     * 最后同步时间（最后一次从第三方平台更新信息的时间）
     */
    @Column(name = "last_sync_at")
    private Long lastSyncAt;

    /**
     * 多对一关联到 User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

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
