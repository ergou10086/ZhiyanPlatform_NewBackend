package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.dto.ConversationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationListResponse {

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("has_more")
    private Boolean hasMore;

    @JsonProperty("data")
    private List<ConversationDTO> data;
}
