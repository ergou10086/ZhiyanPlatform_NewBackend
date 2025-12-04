package hbnu.project.zhiyanbackend.ai.aiassistant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DifyAppInfoDTO类，用于封装Dify应用程序信息的数据传输对象
 * 使用了Lombok注解简化代码，包括@Data、@Builder、@NoArgsConstructor和@AllArgsConstructor
 * @JsonInclude注解用于指定在序列化时忽略null值属性
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DifyAppInfoDTO {

    /**
     * 应用程序名称
     * 使用@JsonProperty注解指定JSON序列化/反序列化时的字段名
     */
    @JsonProperty("name")
    private String name;

    /**
     * 应用程序描述信息
     */
    @JsonProperty("description")
    /**
     * 应用程序标签列表
     */
    private String description;

    @JsonProperty("tags")
    /**
     * 应用程序模式
     */
    private List<String> tags;

    @JsonProperty("mode")
    /**
     * 作者名称
     */
    private String mode;

    @JsonProperty("author_name")
    private String authorName;
}
