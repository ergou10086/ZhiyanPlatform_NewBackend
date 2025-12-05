package hbnu.project.zhiyanbackend.auth.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.auth.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "users", schema = "zhiyanauth")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseAuditEntity {

    /**
     * 雪花id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户邮箱（登录账号）
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * 用户密码哈希值
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * 用户名
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 个人简介
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 头像二进制数据（PostgreSQL BYTEA类型）
     * 直接存储在数据库中，不使用对象存储
     */
    @Column(name = "avatar_data", columnDefinition = "BYTEA")
    @JsonIgnore
    private byte[] avatarData;

    /**
     * 头像MIME类型（如：image/jpeg, image/png）
     */
    @Column(name = "avatar_content_type", length = 50)
    private String avatarContentType;

    /**
     * 头像文件大小（字节）
     */
    @Column(name = "avatar_size")
    private Long avatarSize;

    /**
     * 用户职称/职位
     */
    @Column(name = "title", length = 100)
    private String title;

    /**
     * 所属机构
     */
    @Column(name = "institution", length = 200)
    private String institution;

    /**
     * 账号是否锁定
     */
    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    /**
     * 软删除标记
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 研究方向标签（JSON数组格式）
     * 存储格式：["机器学习", "自然语言处理", "大模型"]
     * 最多5个标签
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "research_tags")
    private String researchTags;

    /**
     * 个人关联链接（JSON数组）
     * 存储格式：[{"label":"GitHub","url":"https://github.com/xxx"}, ...]
     * 最多6条
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_links")
    private String profileLinks;

    /**
     * 用户状态
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 上次登录IP地址
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    /**
     * 2FA密钥（Base32编码，用于生成TOTP）
     * 仅在启用2FA时存储
     */
    @Column(name = "two_factor_secret", length = 32)
    @JsonIgnore
    private String twoFactorSecret;

    /**
     * 是否启用双因素认证
     */
    @Builder.Default
    @Column(name = "two_factor_enabled", nullable = false)
    @ColumnDefault("false")
    private Boolean twoFactorEnabled = false;

    /**
     * 用户角色关联（一对多）
     * 注意：
     * - @JsonIgnore: 避免 JSON 序列化时的循环引用
     * - @lombok.ToString.Exclude: 避免 toString() 触发懒加载
     */
    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserRole> userRoles = new ArrayList<>();

    /**
     * 在持久化之前生成雪花ID
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
    }


    /**
     * 获取研究方向标签列表
     */
    @Transient
    public List<String> getResearchTagList() {
        if (researchTags == null || researchTags.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return new ObjectMapper().readValue(researchTags, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }


    /**
     * 设置研究方向标签列表
     */
    @Transient
    public void setResearchTagList(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            this.researchTags = null;
            return;
        }
        // 限制最多5个标签
        List<String> limitedTags = tags.stream().limit(5).collect(Collectors.toList());
        try {
            this.researchTags = new ObjectMapper().writeValueAsString(limitedTags);
        } catch (Exception e) {
            this.researchTags = null;
        }
    }

    /**
     * 获取个人关联链接列表
     */
    @Transient
    public List<hbnu.project.zhiyanbackend.auth.model.dto.ProfileLinkDTO> getProfileLinkList() {
        if (profileLinks == null || profileLinks.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return new ObjectMapper().readValue(
                    profileLinks,
                    new TypeReference<List<hbnu.project.zhiyanbackend.auth.model.dto.ProfileLinkDTO>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 设置个人关联链接列表，限制最多6条
     */
    @Transient
    public void setProfileLinkList(List<hbnu.project.zhiyanbackend.auth.model.dto.ProfileLinkDTO> links) {
        if (links == null || links.isEmpty()) {
            this.profileLinks = null;
            return;
        }

        // 只保留前6条
        List<hbnu.project.zhiyanbackend.auth.model.dto.ProfileLinkDTO> limited =
                links.stream().limit(6).collect(Collectors.toList());
        try {
            this.profileLinks = new ObjectMapper().writeValueAsString(limited);
        } catch (Exception e) {
            this.profileLinks = null;
        }
    }
}