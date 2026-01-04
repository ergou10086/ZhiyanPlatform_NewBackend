package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量推送执行请求
 *
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPushExecuteRequest {

    private Audience audience;

    private String taskid;

    /**
     * 是否异步推送，true是异步，false同步。异步推送不会返回data详情
     */
    @JsonProperty("is_async")
    private Boolean isAsync;
    
    @JsonProperty("need_alias_detail")
    private Boolean needAliasDetail;
}