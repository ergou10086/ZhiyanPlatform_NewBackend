package hbnu.project.zhiyanbackend.sse.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * SSE消息DTO
 * 用于Redis消息发布/订阅和SSE推送
 *
 * @author yui
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SseMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 需要推送到的session key 列表
     */
    private List<Long> userIds;

    /**
     * 需要发送的消息
     */
    private String message;
}
