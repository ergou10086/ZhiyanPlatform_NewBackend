package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送设置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSettings {
    /**
     * 消息离线时间，单位毫秒，-1表示不设离线
     */
    private Long ttl;
    
    /**
     * 厂商通道策略
     */
    private Strategy strategy;
    
    /**
     * 定速推送，单位：条/秒，0表示不限速
     */
    private Integer speed;
    
    /**
     * 定时推送时间，毫秒时间戳
     */
    @JsonProperty("schedule_time")
    private Long scheduleTime;
}