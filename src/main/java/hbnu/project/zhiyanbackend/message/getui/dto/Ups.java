package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UPS通道配置
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ups {

    /**
     * 通知消息内容
     * 目前只放 notification，投传，撤回，options都不先不写
     */
    private UpsNotification notification;
}

