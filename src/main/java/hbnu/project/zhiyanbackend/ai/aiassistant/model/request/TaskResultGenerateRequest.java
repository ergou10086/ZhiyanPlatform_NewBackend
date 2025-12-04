package hbnu.project.zhiyanbackend.ai.aiassistant.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务结果生成请求类
 * 用于封装任务结果生成所需的请求参数
 * 实现了序列化接口，支持对象序列化传输
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultGenerateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;  // 序列化版本UID，用于版本控制

    private Long projectId;  // 项目ID，用于标识所属项目

    private String achievementTitle;  // 成果标题，用于标识生成结果的标题

    @Builder.Default
    private List<Long> taskIds = new ArrayList<>();  // 任务ID列表，默认初始化为空ArrayList

    private String targetAudience;  // 目标受众，描述成果的使用对象  // 额外要求，用于描述生成成果的其他需求

    private String additionalRequirements;
  // 是否包含附件，默认为false
    @Builder.Default
    private Boolean includeAttachments = false;
  // 附件过滤器列表，默认初始化为空ArrayList
    @Builder.Default
    private List<String> attachmentFilters = new ArrayList<>();  // 用户ID，用于标识请求发起人

    private Long userId;
}
