package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知消息
 * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-6
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /**
     * 通知消息标题，长度 ≤ 50字
     */
    private String title;
    
    /**
     * 通知消息内容，长度 ≤ 256字
     */
    private String body;
    
    /**
     * 点击通知后续动作
     * 目前支持以下后续动作，
     * intent：打开应用内特定页面（intent和want字段必须填写一个）
     * url：打开网页地址，
     * payload：自定义消息内容启动应用，
     * payload_custom：自定义消息内容不启动应用，
     * startapp：打开应用首页，
     * none：纯通知，无后续动作
     */
    @JsonProperty("click_type")
    private String clickType;
    
    /**
     * 点击通知后打开的URL
     * 点击通知栏消息时，唤起系统默认浏览器打开此链接。必须填写可访问的链接，url长度 ≤ 1024字
     */
    private String url;
    
    /**
     * 点击通知后的自定义内容
     * 点击通知时，附加自定义透传消息，长度 ≤ 3072字
     */
    private String payload;
    
    /**
     * 点击通知后打开的Activity intent
     * 针对安卓系统设置点击通知打开应用特定页面，长度 ≤ 4096字;
     * 示例：intent://com.getui.push/detail?#Intent;scheme=gtpushscheme;launchFlags=0x4000000;
     * package=com.getui.demo;component=com.getui.demo/
     * com.getui.demo.DemoActivity;S.payload=payloadStr;end
     */
    private String intent;
    
    /**
     * 通知图标URL
     * 通知的图标名称，包含后缀名（需要在客户端开发时嵌入），如“push.png”，长度 ≤ 64字
     */
    private String logo;
    
    /**
     * 通知大图URL
     * 通知图标URL地址，长度 ≤ 256字
     */
    @JsonProperty("logo_url")
    private String logoUrl;
    
    /**
     * 角标数字
     * 角标, 必须大于0, 个推通道下发有效
     * 此属性目前针对：安卓华为、鸿蒙华为(HarmonyOS NEXT)、荣耀 生效
     * 华为EMUI 4.1 及以上设备有效
     * 荣耀版本要求 Magic UI 4.0及以上有效
     * 角标数字数据会和之前角标数字进行叠加；
     * 举例：角标数字配置1，应用之前角标数为2，发送此角标消息后，应用角标数显示为3。
     * 客户端SDK最低要求 2.14.0.0
     */
    private Integer badge_add_num;
}