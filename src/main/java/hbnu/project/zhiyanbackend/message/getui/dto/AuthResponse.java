package hbnu.project.zhiyanbackend.message.getui.dto;
import lombok.Data;

/**
 * 鉴权响应DTO
 *
 * @author ErgouTree
 */
@Data
public class AuthResponse {

    /**
     * 成功或失败code码，详细含义见 https://docs.getui.com/getui/server/rest_v2/code/?id=doc-title-1
     */
    private Integer code;

    /**
     * 失败时返回此说明
     */
    private String msg;

    /**
     * 详见接口说明
     */
    private AuthData data;
}
