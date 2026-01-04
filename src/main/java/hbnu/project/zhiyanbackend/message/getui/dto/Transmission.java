package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 透传消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transmission {

    /**
     * 透传内容
     */
    @JsonProperty("transmission_content")
    private String transmissionContent;
}