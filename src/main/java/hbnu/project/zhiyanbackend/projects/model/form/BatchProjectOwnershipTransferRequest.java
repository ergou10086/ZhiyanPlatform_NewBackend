package hbnu.project.zhiyanbackend.projects.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量项目所有权移交请求
 */
@Data
@Schema(description = "批量项目所有权移交请求")
public class BatchProjectOwnershipTransferRequest {

    @Schema(description = "项目所有权移交请求列表", required = true)
    private List<ProjectOwnershipTransferRequest> transfers;
}
