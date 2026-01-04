package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量推送请求
 * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-3
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPushRequest {

    /**
     * 是否异步推送
     */
    @JsonProperty("is_async")
    private Boolean isAsync;
    
    /**
     * 消息列表
     */
    @JsonProperty("msg_list")
    private List<PushMessageRequest> msgList;
}