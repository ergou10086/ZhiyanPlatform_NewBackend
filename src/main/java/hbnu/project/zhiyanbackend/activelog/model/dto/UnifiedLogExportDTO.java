package hbnu.project.zhiyanbackend.activelog.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一日志导出DTO
 * 用于导出聚合后的操作日志
 *
 * @author ErgouTree
 */
@Data
public class UnifiedLogExportDTO {

    private String id;

    private String projectId;

    private String userId;

    private String username;

    private String operationModule;

    private String operationType;

    private String title;

    private String description;

    private String time;

    private String source;

    private String relatedId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 格式化时间为字符串
     */
    public void setTime(LocalDateTime time) {
        this.time = time != null ? time.format(DATE_FORMATTER) : "";
    }
}
