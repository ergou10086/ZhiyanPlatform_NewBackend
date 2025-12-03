package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * ConversationDTO类，用于表示对话的数据传输对象
 * 使用了Lombok注解简化了getter/setter、构造函数等的编写
 * 同时包含了JSON序列化相关的配置
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationDTO {

    /**
     * 对话的唯一标识符
     * 对应JSON字段中的"id"
     */
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("inputs")
    private Map<String, Object> inputs;

    @JsonProperty("status")
    private String status;

    @JsonProperty("introduction")
    private String introduction;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
