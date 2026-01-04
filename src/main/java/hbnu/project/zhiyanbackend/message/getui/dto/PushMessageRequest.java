package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送消息请求基类
 * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-1
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessageRequest {

    /**
     * 请求唯一标识号，10-32位之间；如果request_id重复，会导致消息丢失
     * 必须，无默认
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * 任务组名。
     * 多个消息任务可以用同一个任务组名，后续可根据任务组名查询推送情况（长度限制100字符，且不能含有特殊符号）只允许填写数字、字母、横杠、下划线
     * 不必须，无默认
     */
    @JsonProperty("group_name")
    private String groupName;

    /**
     * 推送条件设置
     * ttl：Number,默认两小时，此为消息离线时间设置，单位毫秒，-1表示不设离线，-1 ～ 3 * 24 * 3600 * 1000(3天)之间
     * strategy：Json，{"strategy":{"default":1}}，厂商通道策略，详细内容见strategy
     */
    private PushSettings settings;

    /**
     * 推送目标用户
     * 名称cid，类型String Array，必须，无默认，cid数组，只能填一个cid
     */
    private Audience audience;

    /**
     * 个推推送消息参数
     * 详情见其类
     * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-6#doc-title-6
     */
    @JsonProperty("push_message")
    private PushMessage pushMessage;

    /**
     * 厂商推送消息参数，包含ios消息参数，android厂商消息参数
     * 详情见其类
     * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-7#doc-title-7
     */
    @JsonProperty("push_channel")
    private PushChannel pushChannel;
}