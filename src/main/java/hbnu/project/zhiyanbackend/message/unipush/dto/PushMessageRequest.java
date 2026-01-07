package hbnu.project.zhiyanbackend.message.unipush.dto;

import lombok.Data;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 推送消息请求DTO
 * https://doc.dcloud.net.cn/uniCloud/uni-cloud-push/api.html#%E6%8E%A5%E5%8F%A3%E5%BD%A2%E5%BC%8F
 *
 * @author ErgouTree
 */
@Data
@Builder
public class PushMessageRequest {

    /**
     * 推送标题
     */
    private String title;

    /**
     * 推送内容
     */
    private String content;

    /**
     * 消息payload
     * 推送透传数据，app程序接受的数据，长度小于800字符;
     */
    private Map<String, Object> payload;

    /**
     * 客户端ID列表
     * 该平台使用基于 clientId 的形式进行消息推送
     * 支持数组推送
     */
    @JsonProperty("push_clientid")
    private List<String> pushClientId;

    /**
     * 请求唯一标识号
     * 10-32位之间
     * 如果request_id重复，会导致消息丢失
     */
    @JsonProperty("request_id")
    private String requestId;
}
