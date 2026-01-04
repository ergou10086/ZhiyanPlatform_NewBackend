package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 定时任务数据
 *
 * @author ErgouTree
 */
@Data
class ScheduleTaskData {

    @JsonProperty("create_time")
    private String createTime;
    
    @JsonProperty("send_result")
    private String sendResult;
    
    @JsonProperty("push_time")
    private String pushTime;
    
    @JsonProperty("transmission_content")
    private String transmissionContent;
}