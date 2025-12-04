package hbnu.project.zhiyanbackend.activelog.service;

import hbnu.project.zhiyanbackend.activelog.model.entity.AchievementOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.ProjectOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.TaskOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.WikiOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.vo.UnifiedOperationLogVO;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * 个人操作日志服务接口
 * <p>
 * 提供「我的日志」相关查询能力，包含两类核心场景：
 * 1. 我主动操作的日志（userId匹配操作人）
 * 2. 关于我的日志（他人操作涉及我，如任务分配、成员添加等）
 *
 * @author ErgouTree
 * @version 1.4
 * @since 2025-12-04
 */
public interface OperationLogMyselfActionService {

    /**
     * 查询用户在所有项目的所有操作日志（分页）
     * <p>
     * 分页逻辑（高性能优化）：
     * 1. 轻量查询：各日志表仅查询ID和操作时间，减少数据传输
     * 2. 合并排序：跨表合并后按时间倒序排序，保证时序一致性
     * 3. 分页截取：按分页参数截取目标范围的ID列表
     * 4. 批量查询：根据ID列表批量查询日志详情，避免N+1查询
     * 5. 统一转换：转换为UnifiedOperationLogVO返回
     *
     * @param userId   用户ID（必填，操作人ID，非空校验）
     * @param pageable 分页参数（包含页码、页大小，排序规则会被覆盖为按时间倒序）
     * @return Page<UnifiedOperationLogVO> 统一日志VO分页结果，包含：
     *         <ul>
     *             <li>content：日志列表（按操作时间倒序）</li>
     *             <li>totalElements：跨表日志总条数</li>
     *             <li>pageable：分页参数信息</li>
     *         </ul>
     * @throws ServiceException 当用户ID/分页参数为空，或查询过程中出现异常时抛出
     */
    Page<UnifiedOperationLogVO> getProjectAllLogsByMyself(Long userId, Pageable pageable);

    /**
     * 查询用户在所有项目的「项目模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：操作人ID + 时间范围，仅返回项目模块的操作日志
     *
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<ProjectOperationLog> 项目操作日志分页结果
     * @throws ServiceException 当用户ID/分页参数为空时抛出
     */
    Page<ProjectOperationLog> getUserProjectLogs(Long userId, LocalDateTime startTime,
                                                 LocalDateTime endTime, Pageable pageable);

    /**
     * 查询用户在所有项目的「任务模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：操作人ID + 时间范围，仅返回任务模块的操作日志
     *
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<TaskOperationLog> 任务操作日志分页结果
     * @throws ServiceException 当用户ID/分页参数为空时抛出
     */
    Page<TaskOperationLog> getUserTaskLogs(Long userId, LocalDateTime startTime,
                                           LocalDateTime endTime, Pageable pageable);

    /**
     * 查询用户在所有项目的「Wiki模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：操作人ID + 时间范围，仅返回Wiki模块的操作日志
     *
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<WikiOperationLog> Wiki操作日志分页结果
     * @throws ServiceException 当用户ID/分页参数为空时抛出
     */
    Page<WikiOperationLog> getUserWikiLogs(Long userId, LocalDateTime startTime,
                                           LocalDateTime endTime, Pageable pageable);

    /**
     * 查询用户在所有项目的「成果模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：操作人ID + 时间范围，仅返回成果模块的操作日志
     *
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<AchievementOperationLog> 成果操作日志分页结果
     * @throws ServiceException 当用户ID/分页参数为空时抛出
     */
    Page<AchievementOperationLog> getUserAchievementLogs(Long userId, LocalDateTime startTime,
                                                         LocalDateTime endTime, Pageable pageable);

    // ==================== 指定项目的日志查询 ====================

    /**
     * 查询用户在指定项目的所有操作日志（分页）
     * <p>
     * 分页逻辑（高性能优化）：
     * 1. 轻量查询：各日志表仅查询ID和操作时间，减少数据传输
     * 2. 合并排序：跨表合并后按时间倒序排序，保证时序一致性
     * 3. 分页截取：按分页参数截取目标范围的ID列表
     * 4. 批量查询：根据ID列表批量查询日志详情，避免N+1查询
     * 5. 统一转换：转换为UnifiedOperationLogVO返回
     *
     * @param projectId 项目ID（必填，非空校验）
     * @param userId    用户ID（必填，操作人ID，非空校验）
     * @param pageable  分页参数（包含页码、页大小，排序规则会被覆盖为按时间倒序）
     * @return Page<UnifiedOperationLogVO> 统一日志VO分页结果，包含：
     *         <ul>
     *             <li>content：日志列表（按操作时间倒序）</li>
     *             <li>totalElements：跨表日志总条数</li>
     *             <li>pageable：分页参数信息</li>
     *         </ul>
     * @throws ServiceException 当项目ID/用户ID/分页参数为空，或查询过程中出现异常时抛出
     */
    Page<UnifiedOperationLogVO> getProjectAllLogsByMyself(Long projectId, Long userId, Pageable pageable);

    /**
     * 查询用户在指定项目的「项目模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：项目ID + 操作人ID + 时间范围，仅返回项目模块的操作日志
     *
     * @param projectId 项目ID（必填）
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<ProjectOperationLog> 项目操作日志分页结果
     * @throws ServiceException 当项目ID/用户ID/分页参数为空时抛出
     */
    Page<ProjectOperationLog> getUserProjectLogsInProject(Long projectId, Long userId,
                                                          LocalDateTime startTime, LocalDateTime endTime,
                                                          Pageable pageable);

    /**
     * 查询用户在指定项目的「任务模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：项目ID + 操作人ID + 时间范围，仅返回任务模块的操作日志
     *
     * @param projectId 项目ID（必填）
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<TaskOperationLog> 任务操作日志分页结果
     * @throws ServiceException 当项目ID/用户ID/分页参数为空时抛出
     */
    Page<TaskOperationLog> getUserTaskLogsInProject(Long projectId, Long userId,
                                                    LocalDateTime startTime, LocalDateTime endTime,
                                                    Pageable pageable);

    /**
     * 查询用户在指定项目的「Wiki模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：项目ID + 操作人ID + 时间范围，仅返回Wiki模块的操作日志
     *
     * @param projectId 项目ID（必填）
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<WikiOperationLog> Wiki操作日志分页结果
     * @throws ServiceException 当项目ID/用户ID/分页参数为空时抛出
     */
    Page<WikiOperationLog> getUserWikiLogsInProject(Long projectId, Long userId,
                                                    LocalDateTime startTime, LocalDateTime endTime,
                                                    Pageable pageable);

    /**
     * 查询用户在指定项目的「成果模块」操作日志（带时间筛选）
     * <p>
     * 筛选维度：项目ID + 操作人ID + 时间范围，仅返回成果模块的操作日志
     *
     * @param projectId 项目ID（必填）
     * @param userId    用户ID（必填，操作人ID）
     * @param startTime 操作开始时间（可选，>= 该时间）
     * @param endTime   操作结束时间（可选，<= 该时间）
     * @param pageable  分页参数（必填，排序规则会被覆盖为按操作时间倒序）
     * @return Page<AchievementOperationLog> 成果操作日志分页结果
     * @throws ServiceException 当项目ID/用户ID/分页参数为空时抛出
     */
    Page<AchievementOperationLog> getUserAchievementLogsInProject(Long projectId, Long userId,
                                                                  LocalDateTime startTime, LocalDateTime endTime,
                                                                  Pageable pageable);
}