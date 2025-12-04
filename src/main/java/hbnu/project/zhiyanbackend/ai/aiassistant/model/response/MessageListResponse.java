package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.dto.DifyMessageDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息列表响应类
 * 使用了Lombok注解自动生成getter、setter、toString等方法
 * 使用了Builder模式构建对象
 * 提供了无参和全参构造函数
 * 配置了JSON序列化时忽略null值的属性
 *
 * @author Tokito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageListResponse {

    /**
     * 分页限制数量
     * 对应JSON中的"limit"字段
     */
    @JsonProperty("limit")
    private Integer limit;
    /**
     * 是否有更多数据
     * 对应JSON中的"has_more"字段
     */

    @JsonProperty("has_more")
    /**
     * 消息数据列表
     * 对应JSON中的"data"字段
     * 包含DifyMessageDTO类型的消息对象集合
     */
    private Boolean hasMore;

    @JsonProperty("data")
    private List<DifyMessageDTO> data;
}
