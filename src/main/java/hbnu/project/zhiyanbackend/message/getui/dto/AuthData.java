package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 鉴权数据
 *
 * @author ErgouTree
 */
@Data
public class AuthData {

    /**
     * token过期时间，ms时间戳
     */
    @JsonProperty("expire_time")
    private String expireTime;

    /**
     * 接口调用凭据
     */
    private String token;
}
