package hbnu.project.zhiyanbackend.oss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 删除操作结果响应对象
 *
 * @author ErgouTree
 */
@Data
@Builder
@Schema(description = "删除操作结果")
public class DeleteResultDTO {

    @Schema(description = "总数量")
    private Integer totalCount;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failedCount;

    @Schema(description = "成功删除的对象键列表")
    private List<String> successKeys;

    @Schema(description = "删除失败的对象列表")
    private List<FailedObject> failedObjects;

    /**
     * 删除失败的对象信息
     */
    @Data
    @Builder
    @Schema(description = "删除失败的对象")
    public static class FailedObject {
        
        @Schema(description = "对象键")
        private String key;
        
        @Schema(description = "错误代码")
        private String code;
        
        @Schema(description = "错误消息")
        private String message;
        
        @Schema(description = "版本ID(如果有,但是目前没有)")
        private String versionId;
    }
}