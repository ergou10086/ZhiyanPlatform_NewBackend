package hbnu.project.zhiyanbackend.activelog.model.dto;

import cn.idev.excel.annotation.ExcelProperty;
import hbnu.project.zhiyanactivelog.model.enums.OperationResult;
import hbnu.project.zhiyanactivelog.model.enums.ProjectOperationType;
import hbnu.project.zhiyancommonexcel.annotation.ExcelEnumFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 项目日志导出DTO
 *
 * @author ErgouTree
 */
@Data
public class ProjectLogExportDTO {

    private String id;

    private String projectId;

    private String projectName;

    private String userId;

    private String username;

    private String operationType;

    private String operationModule;

    private String operationDesc;

    private String operationResult;

    private String operationTime;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 格式化时间为字符串
     */
    public void setOperationTime(LocalDateTime time) {
        this.operationTime = time != null ? time.format(DATE_FORMATTER) : "";
    }
}