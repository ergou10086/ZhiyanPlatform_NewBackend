package hbnu.project.zhiyanbackend.basic.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * 提供统一的审计字段和乐观锁支持
 *
 * @author ErgouTree
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    /**
     * 数据创建时间（由审计自动填充）
     */
    @JsonIgnore
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 数据最后修改时间（由审计自动更新）
     */
    @JsonIgnore
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * 数据创建人ID（由审计自动填充）
     */
    @CreatedBy
    @LongToString
    @Column(name = "created_by", columnDefinition = "BIGINT")
    private Long createdBy;

    /**
     * 数据最后修改人ID（由审计自动更新）
     */
    @LastModifiedBy
    @LongToString
    @Column(name = "updated_by", columnDefinition = "BIGINT")
    private Long updatedBy;


    /**
     * 版本号（乐观锁）
     */
    @Version
    @Column(name = "version", nullable = false,
            columnDefinition = "INTEGER DEFAULT 0")
    private Integer version = 0;
}
