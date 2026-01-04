package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UPS通知配置
 * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-7
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsNotification {

    /**
     * 通知栏标题（中英文都只算一个字符，长度建议取最小集）
     * 例如
     * 小米：title长度限制为 50 字符
     * OPPO：title长度限制 50 字符
     * VIVO：title长度限制 40 字符
     */
    private String title;

    /**
     * 通知栏内容(中英文都只算一个字符，长度建议取最小集)
     * 例如
     * HW：content长度限制 256 字符
     * 小米：content长度限制 128 字符
     * OPPO：content长度限制 200 字符
     * VIVO：content长度限制 100 字符
     */
    private String body;

    /**
     * 点击通知后续动作,
     * 目前支持以下后续动作，
     * intent：打开应用内特定页面(厂商都支持)，
     * url：打开网页地址(厂商都支持；华为/荣耀要求https协议，且游戏类应用不支持打开网页地址)，
     * startapp：打开应用首页(厂商都支持)
     */
    @JsonProperty("click_type")
    private String clickType;

    /**
     * 点击通知栏消息时，唤起系统默认浏览器打开此链接。必须填写可访问的链接，url长度 ≤ 2048 字符(中英文都只算一个字符)
     */
    private String url;

    /**
     * 点击通知打开应用特定页面
     * intent格式必须正确且不能为空，长度 ≤ 4096 字符(中英文都只算一个字符);
     * 示例： intent://com.getui.push/detail?#Intent;scheme=gtpushscheme;launchFlags=0x4000000;package=com.getui.demo;component=com.getui.demo/com.getui.demo.DemoActivity;S.payload=payloadStr;S.gttask=;end
     * intent生成请参考 https://docs.getui.com/getui/mobile/vendor/androidstudio/?id=doc-title-7
     */
    private String intent;
}
