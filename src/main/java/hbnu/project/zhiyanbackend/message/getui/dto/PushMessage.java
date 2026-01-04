package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 推送消息内容
 * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-6
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {

    /**
     * 通知消息内容
     * 安卓系统和鸿蒙系统支持
     */
    private Notification notification;
    
    /**
     * 纯透传消息内容
     * 安卓、鸿蒙和iOS均支持，与 notification、revoke 三选一，都填写时报错
     */
    private Transmission transmission;
}