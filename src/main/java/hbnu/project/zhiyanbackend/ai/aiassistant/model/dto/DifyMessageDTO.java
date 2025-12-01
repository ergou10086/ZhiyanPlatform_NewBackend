package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DifyMessageDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("inputs")
    private Map<String, Object> inputs;

    @JsonProperty("query")
    private String query;

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("message_files")
    private List<MessageFile> messageFiles;

    @JsonProperty("feedback")
    private Feedback feedback;

    @JsonProperty("retriever_resources")
    private List<RetrieverResource> retrieverResources;

    @JsonProperty("created_at")
    private Long createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageFile {

        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("url")
        private String url;

        @JsonProperty("belongs_to")
        private String belongsTo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Feedback {

        @JsonProperty("rating")
        private String rating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RetrieverResource {

        @JsonProperty("position")
        private Integer position;

        @JsonProperty("dataset_id")
        private String datasetId;

        @JsonProperty("dataset_name")
        private String datasetName;

        @JsonProperty("document_id")
        private String documentId;

        @JsonProperty("document_name")
        private String documentName;

        @JsonProperty("segment_id")
        private String segmentId;

        @JsonProperty("score")
        private Double score;

        @JsonProperty("content")
        private String content;
    }
}
