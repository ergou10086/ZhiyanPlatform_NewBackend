package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Dify消息数据传输对象(DTO)，用于封装与Dify API交互的消息数据
 * 使用Lombok注解简化样板代码，包括getter/setter、builder模式、无参构造器和全参构造器
 * 使用Jackson注解控制JSON序列化行为，忽略null值字段
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DifyMessageDTO {

    /**
     * 消息唯一标识符
     */
    @JsonProperty("id")
    private String id;

    /**
     * 对话ID，标识消息所属的对话
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 输入参数映射，包含处理消息所需的各类参数
     */
    @JsonProperty("inputs")
    private Map<String, Object> inputs;
    /**
     * 用户查询内容
     */

    @JsonProperty("query")
    /**
     * 系统返回的答案内容
     */
    private String query;

    @JsonProperty("answer")
    /**
     * 消息关联的文件列表
     */
    private String answer;

    @JsonProperty("message_files")
    /**
     * 消息反馈信息
     */
    private List<MessageFile> messageFiles;

    @JsonProperty("feedback")
    /**
     * 检索到的资源列表
     */
    private Feedback feedback;

    @JsonProperty("retriever_resources")
    /**
     * 消息创建时间戳
     */
    private List<RetrieverResource> retrieverResources;

    @JsonProperty("created_at")
    /**
     * 消息文件内部类，封装文件相关信息
     */
    private Long createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
        /**
         * 文件唯一标识符
         */
    public static class MessageFile {

        @JsonProperty("id")
        /**
         * 文件类型
         */
        private String id;

        @JsonProperty("type")
        /**
         * 文件访问URL
         */
        private String type;

        @JsonProperty("url")
        /**
         * 文件所属关系标识
         */
        private String url;

        @JsonProperty("belongs_to")
        private String belongsTo;
    /**
     * 反馈信息内部类，封装用户反馈相关数据
     */
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
        /**
         * 用户评分
         */
    public static class Feedback {

        @JsonProperty("rating")
        private String rating;
    /**
     * 检索资源内部类，封装检索到的资源相关信息
     */
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
        /**
         * 资源在检索结果中的位置
         */
    public static class RetrieverResource {

        @JsonProperty("position")
        /**
         * 数据集ID
         */
        private Integer position;

        @JsonProperty("dataset_id")
        /**
         * 数据集名称
         */
        private String datasetId;

        @JsonProperty("dataset_name")
        /**
         * 文档ID
         */
        private String datasetName;

        @JsonProperty("document_id")
        /**
         * 文档名称
         */
        private String documentId;

        @JsonProperty("document_name")
        /**
         * 文档段ID
         */
        private String documentName;

        @JsonProperty("segment_id")
        /**
         * 检索得分
         */
        private String segmentId;

        @JsonProperty("score")
        /**
         * 资源内容
         */
        private Double score;

        @JsonProperty("content")
        private String content;
    }
}
