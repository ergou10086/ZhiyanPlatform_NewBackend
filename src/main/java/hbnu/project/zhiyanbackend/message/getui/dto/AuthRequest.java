package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 鉴权请求DTO
 * 参考 https://docs.getui.com/getui/server/rest_v2/token/
 * 其中 body 参数说明
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /**
     * 鉴权时的签名
     * 生成 sign 值：将 appkey、timestamp、mastersecret 对应的字符串按此固定顺序拼接后，使用 SHA256 算法加密。
     * 示例 java 代码格式: String sign = sha256(appkey+timestamp+mastersecret)
     */
    private String sign;

    /**
     * 毫秒时间戳（13位），请使用当前毫秒时间戳，误差太大可能出错
     */
    private String timestamp;

    /**
     * 创建应用时生成的appkey
     */
    private String appkey;
}
