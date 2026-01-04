package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 厂商通道策略
 * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-5#doc-title-5
 * 厂商推送策略为 VIP 功能，需升级服务后方可使用。非VIP用户无法设置厂商策略，无需填写strategy 字段，默认所有通道策略都是1 。若须申请修改请点击右侧“技术咨询”了解详情。
 * 但是留着，便于完整参考
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Strategy {

    /**
     * 默认策略：1-表示该消息在用户在线时推送个推通道，用户离线时推送厂商通道
     * 2-表示该消息只通过厂商通道策略下发，不考虑用户是否在线
     * 3-表示该消息只通过个推通道下发，不考虑用户是否在线
     * 4-表示该消息优先从厂商通道下发，若消息内容在厂商通道不支持则会通过个推通道下发
     */
    @JsonProperty("default")
    private Integer defaultStrategy;
}