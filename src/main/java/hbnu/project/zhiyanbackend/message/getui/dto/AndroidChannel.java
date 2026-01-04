package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Android厂商推送通道
 * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-7#doc-title-7
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AndroidChannel {

    /**
     * 厂商消息分类，打算支持华为、小米
     */
    private Ups ups;
}