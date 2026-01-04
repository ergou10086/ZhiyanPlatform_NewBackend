package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.Data;

import java.util.Map;

/**
 * 定时任务查询响应
 *
 * @author ErgouTree
 */
@Data
public class ScheduleTaskResponse {
    private Integer code;
    private String msg;
    private Map<String, ScheduleTaskData> data;
}