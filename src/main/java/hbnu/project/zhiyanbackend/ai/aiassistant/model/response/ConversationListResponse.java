package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.dto.ConversationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ConversationListResponse类用于封装会话列表的响应数据
 * 使用了Lombok注解简化代码，包括@Data、@Builder、@NoArgsConstructor和@AllArgsConstructor
 * @JsonInclude注解确保在序列化为JSON时，忽略值为null的属性
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationListResponse {

    /**
     * 分页查询时的每页条目数限制
     * 对应JSON中的"limit"字段
     */
    @JsonProperty("limit")
    /**
     * 是否存在更多数据的标志
     * 对应JSON中的"has_more"字段
     * true表示还有更多数据，false表示已显示所有数据
     */
    private Integer limit;

    @JsonProperty("has_more")
    /**
     * 会话列表数据
     * 对应JSON中的"data"字段
     * 包含多个ConversationDTO对象，每个对象代表一个会话的详细信息
     */
    private Boolean hasMore;

    @JsonProperty("data")
    private List<ConversationDTO> data;
}
