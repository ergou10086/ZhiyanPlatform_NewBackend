package hbnu.project.zhiyanbackend.activelog.model.entity;

import hbnu.project.zhiyanbackend.activelog.model.enums.WikiOperationType;
import hbnu.project.zhiyanbackend.basic.annotation.LongToString;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Wiki操作日志实体类
 *
 * @author ErgouTree
 */
@Entity
@Table(name = "wiki_operation_log", schema = "zhiyanactivelog"
        ,indexes = {
            @Index(name = "idx_wiki_operation_log_project_id", columnList = "project_id"),
            @Index(name = "idx_wiki_operation_log_wiki_page_id", columnList = "wiki_page_id"),
            @Index(name = "idx_wiki_operation_log_user_id", columnList = "user_id"),
            @Index(name = "idx_wiki_operation_log_operation_type", columnList = "operation_type"),
            @Index(name = "idx_wiki_operation_log_operation_time", columnList = "operation_time"),
            @Index(name = "idx_wiki_operation_log_project_wiki_time", columnList = "project_id, wiki_page_id, operation_time"),
            @Index(name = "idx_wiki_operation_log_project_user_time", columnList = "project_id, user_id, operation_time")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WikiOperationLog {

    /**
     * wiki日志id
     */
    @Id
    @LongToString
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 项目id
     */
    @LongToString
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 操作的wiki页面id
     */
    @LongToString
    @Column(name = "wiki_page_id")
    private Long wikiPageId;

    /**
     * 操作的wiki页面标题
     * 加速字段
     */
    @Column(name = "wiki_page_title", length = 500)
    private String wikiPageTitle;

    /**
     * 操作的用户id
     */
    @LongToString
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 用户名
     * 加速字段
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * wiki的操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private WikiOperationType operationType;

    /**
     * 操作的所属业务
     * 冗余字段
     */
    @Column(name = "operation_module", nullable = false, length = 50)
    private String operationModule = "知识库Wiki管理";

    /**
     * 操作描述
     * 详细说明操作内容（如"编辑Wiki页面：《Spring Boot入门》，修改内容摘要"）
     */
    @Column(name = "operation_desc", length = 500) // 映射字段operation_desc，长度500字符
    private String operationDesc;

    /**
     * 操作时间
     * 记录操作执行的时间
     */
    @Column(name = "operation_time", nullable = false) // 映射字段operation_time，非空约束
    private LocalDateTime operationTime;

    /**
     * 自动生成ID和操作时间（避免手动设置，简化代码）
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = SnowflakeIdUtils.nextId();
        }
        if (this.operationTime == null) {
            this.operationTime = LocalDateTime.now();
        }
    }
}
