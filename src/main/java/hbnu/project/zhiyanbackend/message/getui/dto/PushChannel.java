package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 厂商推送通道
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushChannel {
    
    /**
     * Android厂商推送参数
     * android通道推送消息内容
     */
    private AndroidChannel android;
}