package hbnu.project.zhiyanbackend.knowledge.model.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 成果详情表实体类
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Entity
@Table(name = "achievement_detail", schema = "zhiyanknowledge")
@DynamicInsert
@DynamicUpdate
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDetail extends BaseAuditEntity {

    /**
     * 详情唯一标识
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    private Long id;

    /**
     * 关联的成果ID
     */
    @LongToString
    @Column(name = "achievement_id", nullable = false, unique = true, columnDefinition = "BIGINT")
    private Long achievementId;

    /**
     * 详细信息JSON
     * 示例结构：
     * 论文: {"authors": [], "journal": "", "abstract": "", "doi": ""}
     * 专利: {"patent_no": "", "inventors": [], "application_date": ""}
     * 数据集: {"description": "", "version": "", "format": "", "size": ""}
     * 模型: {"framework": "", "version": "", "purpose": ""}
     */
    @JsonRawValue
    @Column(name = "detail_data", nullable = false, columnDefinition = "JSONB")
    private String detailData;

    /**
     * 摘要/描述（冗余存储，便于搜索）
     */
    @Column(name = "abstract", columnDefinition = "TEXT")
    private String abstractText;


    /**
     * 关联的成果实体（外键关联）
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "achievement_detail_ibfk_1"))
    private Achievement achievement;


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
