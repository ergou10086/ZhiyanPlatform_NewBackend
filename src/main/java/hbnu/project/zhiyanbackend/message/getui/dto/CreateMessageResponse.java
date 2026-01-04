package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.Data;

/**
 * 创建消息响应
 * https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-1
 *
 * @author ErgouTree
 */
@Data
public class CreateMessageResponse {

    /**
     * 成功或失败code码，详细含义见业务返回码说明
     */
    private Integer code;

    /**
     *  	失败时返回此说明
     */
    private String msg;

    /**
     * 详见其类
     */
    private CreateMessageData data;
}