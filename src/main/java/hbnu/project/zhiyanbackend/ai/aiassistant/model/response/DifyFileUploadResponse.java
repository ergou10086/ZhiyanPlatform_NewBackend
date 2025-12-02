package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DifyFileUploadResponse {

    @JsonProperty("id")
    private String fileId;

    @JsonProperty("name")
    private String fileName;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("size")
    private Long fileSize;
}
