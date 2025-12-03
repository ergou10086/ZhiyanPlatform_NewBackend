package hbnu.project.zhiyanbackend.activelog.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聚合后的统一日志视图
 * @author yui
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedOperationLogVO {

    /**
     * 日志id
     */
    private Long id;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 业务模块
     * 项目管理/任务管理/Wiki管理/成果管理/登录
     */
    private String operationModule;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 日志标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 统一时间字段
     */
    private LocalDateTime time;

    /**
     * 来源：PROJECT/TASK/WIKI/ACHIEVEMENT/LOGIN
     */
    private String source;

    /**
     * 可能的业务ID（任务/成果/Wiki）
     */
    private Long relatedId;
}
