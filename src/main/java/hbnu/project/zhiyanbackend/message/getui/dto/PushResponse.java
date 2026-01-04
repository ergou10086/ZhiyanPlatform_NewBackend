package hbnu.project.zhiyanbackend.message.getui.dto;

import lombok.Data;

import java.util.Map;

/**
 * 推送响应
 * 参考公共返回结构
 * @author ErgouTree
 */
@Data
public class PushResponse {

    /**
     * 成功失败码
     */
    private Integer code;

    /**
     * 失败时返回此说明
     */
    private String msg;

    /**
     * 详见推送说明
     */
    private Map<String, Map<String, String>> data;
}