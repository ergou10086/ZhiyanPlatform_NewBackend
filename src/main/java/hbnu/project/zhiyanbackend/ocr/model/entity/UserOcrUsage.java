package hbnu.project.zhiyanbackend.ocr.model.entity;

import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.domain.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import hbnu.project.zhiyanbackend.ocr.model.enums.OcrType;

import java.time.LocalDate;

/**
 * 用户OCR使用记录实体
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "user_ocr_usage", schema = "zhiyanauth",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "ocr_type", "usage_date"})
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserOcrUsage extends BaseAuditEntity {

    /**
     * 雪花id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * OCR类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_type", nullable = false, length = 50)
    private OcrType ocrType;

    /**
     * 使用日期（按天统计）
     */
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    /**
     * 当日使用次数
     */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }

    /**
     * 重置为今天的使用记录
     */
    public void resetForToday() {
        this.usageDate = LocalDate.now();
        this.usageCount = 1;
    }
}
