package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.Data;

/**
 * 创建消息数据
 */
@Data
public class CreateMessageData {

    /**
     * 任务编号
     * 返回值示例
     * {
     *   "code": 0,
     *   "msg": "",
     *   "data": {
     *     "$taskid": {o
     *       "$cid": "$status"
     *     }
     *   }
     * }
     */
    private String taskid;
}
