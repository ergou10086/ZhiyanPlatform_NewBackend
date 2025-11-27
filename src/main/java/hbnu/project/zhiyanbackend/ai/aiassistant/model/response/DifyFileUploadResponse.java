package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import lombok.Data;

@Data
public class DifyFileUploadResponse {

    private String fileId;

    private String fileName;

    private String mimeType;

    private Long fileSize;
}
