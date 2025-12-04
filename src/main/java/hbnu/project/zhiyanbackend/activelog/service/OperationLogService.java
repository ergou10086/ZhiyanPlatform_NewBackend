package hbnu.project.zhiyanbackend.activelog.service;

import hbnu.project.zhiyanbackend.activelog.model.entity.AchievementOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.ProjectOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.TaskOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.WikiOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.vo.UnifiedOperationLogVO;
import hbnu.project.zhiyanbackend.activelog.service.impl.OperationLogServiceImpl;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * 操作日志服务接口
 * <p>
 * 提供项目内各业务模块（项目、任务、Wiki、成果）操作日志的查询能力，
 * 包含单模块精准查询、多模块合并分页查询，支持多维度筛选条件（操作人、时间范围、操作类型等）
 *
 * @author ErgouTree
 * @version 1.0
 * @since 2025-12-04
 */
public interface OperationLogService {

    /**
     * 查询项目内所有类型的操作日志（分页）
     * <p>
     * 核心逻辑：
     * 1. 轻量查询各日志表的ID和操作时间（减少数据传输）
     * 2. 跨表合并后按时间倒序排序
     * 3. 分页截取目标数据后批量查询详情（避免N+1查询）
     * 4. 转换为统一VO返回，保证前端展示格式一致
     * <p>
     * 适用场景：项目详情页的「全部操作日志」展示
     *
     * @param projectId 项目ID（必填，非空校验）
     * @param pageable  分页参数（包含页码、页大小、排序规则，必填）
     * @return Page<UnifiedOperationLogVO> 统一日志VO分页结果，包含：
     *         <ul>
     *             <li>content：日志详情列表（按操作时间倒序）</li>
     *             <li>totalElements：跨表日志总条数</li>
     *             <li>pageable：分页参数信息</li>
     *         </ul>
     * @throws ServiceException 当项目ID为空、分页参数为空或查询过程中出现异常时抛出
     */
    Page<UnifiedOperationLogVO> getProjectAllLogs(Long projectId, Pageable pageable);

    /**
     * 通用方法：查询项目内指定类型的日志（带筛选）
     * <p>
     * 泛型通用查询逻辑，封装重复的分页、排序、基础筛选逻辑，
     * 支持传入额外的查询条件（如任务ID、Wiki页面ID、成果ID等），
     * 为各业务模块日志查询提供底层支撑
     *
     * @param <T>            日志实体类型（如ProjectOperationLog、TaskOperationLog等）
     * @param task           日志查询任务封装（包含实体类、Repository、VO转换函数、时间字段名）
     * @param projectId      项目ID（必填，非空校验）
     * @param operationType  操作类型字符串（可选，不同模块对应不同枚举，如ProjectOperationType）
     * @param username       操作人用户名（可选，支持模糊匹配）
     * @param startTime      操作开始时间（可选，>= 该时间）
     * @param endTime        操作结束时间（可选，<= 该时间）
     * @param pageable       分页参数（必填，包含页码、页大小）
     * @param extraPredicate 额外查询条件构建器（可选，用于补充模块特有条件，如taskId、wikiPageId等）
     * @return Page<T> 指定类型的日志实体分页结果，按操作时间倒序排序
     * @throws ServiceException 当项目ID或分页参数为空时抛出
     */
    <T> Page<T> queryProjectLogsWithFilter(OperationLogServiceImpl.LogQueryTask<T> task, Long projectId,
                                           String operationType, String username,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           Pageable pageable,
                                           Function<Specification<T>, Specification<T>> extraPredicate);

    /**
     * 查询项目内「项目业务模块」的操作日志（带筛选）
     * <p>
     * 筛选维度：项目ID、操作类型、操作人、时间范围，
     * 操作类型会转换为ProjectOperationType枚举进行精准匹配，
     * 无效的操作类型会被忽略并打印警告日志
     * <p>
     * 适用场景：项目模块专属日志查询、操作审计
     *
     * @param projectId      项目ID（必填，非空校验）
     * @param operationType  项目操作类型字符串（可选，如CREATE、UPDATE、DELETE等）
     * @param username       操作人用户名（可选，支持模糊匹配）
     * @param startTime      操作开始时间（可选，>= 该时间）
     * @param endTime        操作结束时间（可选，<= 该时间）
     * @param pageable       分页参数（必填，包含页码、页大小）
     * @return Page<ProjectOperationLog> 项目操作日志分页结果，按操作时间倒序排序
     * @throws ServiceException 当项目ID或分页参数为空时抛出
     */
    Page<ProjectOperationLog> getProjectLogs(Long projectId, String operationType, String username,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             Pageable pageable);

    /**
     * 查询项目内「Wiki业务模块」的操作日志（带筛选）
     * <p>
     * 筛选维度：项目ID、Wiki页面ID、操作类型、操作人、时间范围，
     * 操作类型会转换为WikiOperationType枚举进行精准匹配，
     * 支持查询指定Wiki页面的专属日志
     * <p>
     * 适用场景：Wiki页面详情页日志展示、Wiki操作审计
     *
     * @param projectId      项目ID（必填，非空校验）
     * @param wikiPageId     Wiki页面ID（可选，为空时查询项目内所有Wiki日志）
     * @param operationType  Wiki操作类型字符串（可选，如CREATE、EDIT、DELETE等）
     * @param username       操作人用户名（可选，支持模糊匹配）
     * @param startTime      操作开始时间（可选，>= 该时间）
     * @param endTime        操作结束时间（可选，<= 该时间）
     * @param pageable       分页参数（必填，包含页码、页大小）
     * @return Page<WikiOperationLog> Wiki操作日志分页结果，按操作时间倒序排序
     * @throws ServiceException 当项目ID或分页参数为空时抛出
     */
    Page<WikiOperationLog> getWikiLogs(Long projectId, Long wikiPageId, String operationType,
                                       String username, LocalDateTime startTime, LocalDateTime endTime,
                                       Pageable pageable);

    /**
     * 查询项目内「成果业务模块」的操作日志（带筛选）
     * <p>
     * 筛选维度：项目ID、成果ID、操作类型、操作人、时间范围，
     * 操作类型会转换为AchievementOperationType枚举进行精准匹配，
     * 支持查询指定成果的专属日志
     * <p>
     * 适用场景：成果详情页日志展示、成果操作审计
     *
     * @param projectId      项目ID（必填，非空校验）
     * @param achievementId  成果ID（可选，为空时查询项目内所有成果日志）
     * @param operationType  成果操作类型字符串（可选，如SUBMIT、PUBLISH、OBSOLETE等）
     * @param username       操作人用户名（可选，支持模糊匹配）
     * @param startTime      操作开始时间（可选，>= 该时间）
     * @param endTime        操作结束时间（可选，<= 该时间）
     * @param pageable       分页参数（必填，包含页码、页大小）
     * @return Page<AchievementOperationLog> 成果操作日志分页结果，按操作时间倒序排序
     * @throws ServiceException 当项目ID或分页参数为空时抛出
     */
    Page<AchievementOperationLog> getAchievementLogs(Long projectId, Long achievementId,
                                                     String operationType, String username,
                                                     LocalDateTime startTime, LocalDateTime endTime,
                                                     Pageable pageable);

    /**
     * 查询项目内「任务业务模块」的操作日志（带筛选）
     * <p>
     * 筛选维度：项目ID、任务ID、操作类型、操作人、时间范围，
     * 操作类型字符串会统一转大写后匹配TaskOperationType枚举，增强容错性，
     * 无效的操作类型会被忽略并打印警告日志
     * <p>
     * 适用场景：任务详情页日志展示、任务操作审计
     *
     * @param projectId      项目ID（必填，非空校验）
     * @param taskId         任务ID（可选，为空时查询项目内所有任务日志）
     * @param operationType  任务操作类型字符串（可选，如CREATE、ASSIGN、COMPLETE等）
     * @param username       操作人用户名（可选，支持模糊匹配）
     * @param startTime      操作开始时间（可选，>= 该时间）
     * @param endTime        操作结束时间（可选，<= 该时间）
     * @param pageable       分页参数（必填，包含页码、页大小）
     * @return Page<TaskOperationLog> 任务操作日志分页结果，按操作时间倒序排序
     * @throws ServiceException 当项目ID或分页参数为空时抛出
     */
    Page<TaskOperationLog> getTaskLogs(Long projectId, Long taskId, String operationType,
                                       String username, LocalDateTime startTime, LocalDateTime endTime,
                                       Pageable pageable);
}