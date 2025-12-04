package hbnu.project.zhiyanbackend.ai.aiassistant.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Dify文件上传响应类
 * 用于封装文件上传后返回的数据信息
 * 使用@Data注解自动生成getter、setter等方法
 *
 * @author Tokito
 */
@Data
public class DifyFileUploadResponse {

    /**
     * 文件唯一标识ID
     * 对应JSON字段中的"id"
     */
    @JsonProperty("id")
    private String fileId;

    /**
     * 文件名称
     * 对应JSON字段中的"name"
     */
    @JsonProperty("name")
    private String fileName;

    /**
     * 文件的MIME类型
     * 对应JSON字段中的"mime_type"
     */
    @JsonProperty("mime_type")
    private String mimeType;

    /**
     * 文件大小，以字节为单位
     * 对应JSON字段中的"size"
     */
    @JsonProperty("size")
    private Long fileSize;
}
