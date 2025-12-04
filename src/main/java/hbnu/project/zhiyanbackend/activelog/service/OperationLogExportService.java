package hbnu.project.zhiyanbackend.activelog.service;

import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;

/**
 * 操作日志导出服务接口
 * <p>
 * 提供操作日志的Excel导出功能，支持：
 * 1. 项目操作日志导出（支持筛选条件）
 * 2. 我的操作日志导出（所有项目，支持时间筛选）
 * 3. 我在指定项目的操作日志导出
 *
 * @author ErgouTree
 * @version 1.0
 * @since 2025-12-04
 */
public interface OperationLogExportService {

    /**
     * 导出项目操作日志
     * 可以指定导出的条数，不指定默认全部导出（但不超过最大限制）
     *
     * @param projectId     项目ID（必填）
     * @param operationType 操作类型（可选）
     * @param username      用户名（可选，支持模糊匹配）
     * @param startTime     开始时间（可选）
     * @param endTime       结束时间（可选）
     * @param limit         导出条数限制，null表示不限制（但不超过默认最大值）
     * @param response      HTTP响应
     */
    void exportProjectLogs(Long projectId, String operationType, String username,
                           LocalDateTime startTime, LocalDateTime endTime,
                           Integer limit, HttpServletResponse response);

    /**
     * 导出我的操作日志（所有项目）
     * 可以指定导出的条数，不指定默认全部导出（但不超过最大限制）
     *
     * @param userId    用户ID（必填）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param limit     导出条数限制，null表示不限制（但不超过默认最大值）
     * @param response  HTTP响应
     */
    void exportMyLogs(Long userId, LocalDateTime startTime, LocalDateTime endTime,
                      Integer limit, HttpServletResponse response);

    /**
     * 导出我在指定项目内的所有操作日志
     *
     * @param projectId 项目ID（必填）
     * @param userId   用户ID（必填）
     * @param limit    导出条数限制，null表示不限制（但不超过默认最大值）
     * @param response HTTP响应
     */
    void exportMyProjectLogs(Long projectId, Long userId, Integer limit, HttpServletResponse response);
}
