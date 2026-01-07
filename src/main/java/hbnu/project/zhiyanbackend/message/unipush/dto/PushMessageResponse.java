package hbnu.project.zhiyanbackend.message.unipush.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 推送响应DTO
 * 参考：
 * {
 *     "errCode":0,
 *     "errMsg":"success",
 *     "data": {
 *         "online_statics":{
 *             "$date":4,
 *             "$date":5
 *         }
 *     }
 * }
 *
 * @author ErgouTree
 */
@Data
public class PushMessageResponse {

    /**
     * 失败时返回的错误说明
     */
    private String errMsg;

    /**
     * 错误码
     * 0表示成功
     * https://doc.dcloud.net.cn/uniCloud/uni-cloud-push/api.html#%E8%BF%94%E5%9B%9E%E7%A0%81%E8%AF%B4%E6%98%8E
     */
    @JsonProperty("errCode")
    private Integer errCode;

    /**
     * 任务ID
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 业务数据体
     */
    private UnipushResponseData data;


    /**
     * 嵌套的业务数据DTO
     */
    @Data
    public static class UnipushResponseData {
        /**
         * 在线统计数据：key为时间戳（String类型兼容多格式），value为在线数
         */
        private Map<String, Integer> online_statics;
    }
}
