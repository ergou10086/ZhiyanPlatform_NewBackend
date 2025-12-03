package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务结果生成响应类
 * 用于封装任务生成过程的响应数据
 * 使用了 Lombok 注解简化代码
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultGenerateResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;  // 序列化版本号，用于对象序列化

    private String jobId;          // 任务唯一标识ID

    private String status;         // 任务执行状态

    private Map<String, Object> draftContent;  // 草稿内容，存储生成过程中的中间结果

    private String errorMessage;   // 错误信息，当任务执行失败时记录具体错误原因
  // 任务执行进度，默认为0，使用@Builder.Default设置默认值
    @Builder.Default
    private Integer progress = 0;           // 用户ID，标识任务所属用户

    private Long userId;        // 项目ID，标识任务所属项目

    private Long projectId;

    /**
     * 本次生成涉及的任务ID列表
     * 用于后续将 AI 生成成果落库为任务成果并建立任务关联
     */
    private List<Long> taskIds;

    /**
     * 生成成果的标题（来自请求），用于创建知识库成果时复用
     */
    private String achievementTitle;  // 记录对象创建时间

    private LocalDateTime createdAt;  // 记录对象最后更新时间

    private LocalDateTime updatedAt;
}
