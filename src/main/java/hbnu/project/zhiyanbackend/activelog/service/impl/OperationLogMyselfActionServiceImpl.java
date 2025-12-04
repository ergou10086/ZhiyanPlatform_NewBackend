package hbnu.project.zhiyanbackend.activelog.service.impl;

import hbnu.project.zhiyanbackend.activelog.model.converter.OperationLogConverter;
import hbnu.project.zhiyanbackend.activelog.model.entity.*;
import hbnu.project.zhiyanbackend.activelog.model.vo.UnifiedOperationLogVO;
import hbnu.project.zhiyanbackend.activelog.repository.*;
import hbnu.project.zhiyanbackend.activelog.service.OperationLogMyselfActionService;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 操作日志中我的日志的服务实现
 * 其中，我的日志记录的是 我操作的 和 关于我的（我被别人操作了也要记录，也就是说别人的操作涉及到我了）
 *
 * @author ErgouTree
 * @modify yui,ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogMyselfActionServiceImpl implements OperationLogMyselfActionService {

    // ========== 常量定义 ==========
    /**
     * 操作时间字段名
     */
    private static final String OPERATION_TIME_FIELD = "operationTime";
    /**
     * 默认排序方向（时间倒序）
     */
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;

    private final ProjectOperationLogRepository projectLogRepository;
    private final TaskOperationLogRepository taskLogRepository;
    private final WikiOperationLogRepository wikiLogRepository;
    private final AchievementOperationLogRepository achievementLogRepository;
    private final OperationLogConverter operationLogConverter;

    // ========== 内部记录类 ==========
    /**
     * 日志查询任务定义，记录类
     * 封装实体类型、Repository、VO转换函数、时间字段名，不可变数据载体
     */
    private record LogQueryTask<T>(
            Class<T> entityClass,
            JpaSpecificationExecutor<T> repository,
            Function<T, UnifiedOperationLogVO> mapper,
            String timeField
    ) {}

    /**
     * 日志ID和时间信息，记录类
     * 轻量查询结果载体，包含主键、操作时间、来源表标识
     */
    private record LogIdTimeInfo(
            Long id,
            LocalDateTime time,
            String source // 标识来源表（PROJECT/TASK/WIKI/ACHIEVEMENT）
    ) {}

    // ========== 对外接口实现 ==========
    /**
     * 查询用户在所有项目的所有操作日志（分页）
     */
    @Override
    public Page<UnifiedOperationLogVO> getProjectAllLogsByMyself(Long userId, Pageable pageable) {
        return getProjectAllLogsByMyself(null, userId, pageable);
    }

    /**
     * 查询用户在指定项目的所有操作日志（分页）
     */
    @Override
    public Page<UnifiedOperationLogVO> getProjectAllLogsByMyself(Long projectId, Long userId, Pageable pageable) {
        // 参数校验
        ValidationUtils.requireNonNull(userId, "用户ID不能为空");
        ValidationUtils.requireNonNull(pageable, "分页参数不能为空");
        if (projectId != null) {
            ValidationUtils.requireNonNull(projectId, "项目ID不能为空");
        }

        try {
            // 定义所有需要查询的日志类型
            List<LogQueryTask<?>> queryTasks = buildAllLogQueryTasks();

            // 第一步：轻量查询 - 只查询各表的id和时间
            List<LogIdTimeInfo> allIdTimeInfos = new ArrayList<>();
            for (LogQueryTask<?> task : queryTasks) {
                List<LogIdTimeInfo> idTimeInfos = queryIdAndTime(task, projectId, userId);
                allIdTimeInfos.addAll(idTimeInfos);
            }

            // 第二步：合并排序（按时间倒序）
            allIdTimeInfos.sort(Comparator.comparing(LogIdTimeInfo::time).reversed());

            // 第三步：计算总数和分页范围
            long total = allIdTimeInfos.size();
            int start = pageable.getPageNumber() * pageable.getPageSize();
            int end = Math.min(start + pageable.getPageSize(), allIdTimeInfos.size());

            // 如果超出范围，返回空结果
            if (start >= total) {
                return new PageImpl<>(Collections.emptyList(), pageable, total);
            }

            // 第四步：取分页范围内的id列表
            List<LogIdTimeInfo> pagedIdTimeInfos = allIdTimeInfos.subList(start, end);

            // 第五步：按来源分组，批量查询详情
            Map<String, List<Long>> idsBySource = pagedIdTimeInfos.stream()
                    .collect(Collectors.groupingBy(
                            LogIdTimeInfo::source,
                            Collectors.mapping(LogIdTimeInfo::id, Collectors.toList())
                    ));

            // 第六步：批量查询详情并映射
            List<UnifiedOperationLogVO> result = new ArrayList<>();
            for (LogQueryTask<?> task : queryTasks) {
                String source = getSourceByTask(task);
                List<Long> ids = idsBySource.get(source);
                if (ids != null && !ids.isEmpty()) {
                    result.addAll(batchQueryByIds(task, ids));
                }
            }

            // 第七步：按时间倒序排序
            result.sort(Comparator.comparing(UnifiedOperationLogVO::getTime).reversed());

            return new PageImpl<>(result, pageable, total);
        } catch (Exception e) {
            String errorMsg = projectId == null
                    ? String.format("查询用户所有操作日志失败，用户ID: %s", userId)
                    : String.format("查询用户在指定项目的所有操作日志失败，项目ID: %s, 用户ID: %s", projectId, userId);
            log.error(errorMsg, e);
            throw new ServiceException(errorMsg + ": " + e.getMessage(), e);
        }
    }

    /**
     * 查询用户在所有项目的项目业务模块的操作日志
     */
    @Override
    public Page<ProjectOperationLog> getUserProjectLogs(Long userId, LocalDateTime startTime,
                                                        LocalDateTime endTime, Pageable pageable) {
        return getUserModuleLogsInProject(null, userId, startTime, endTime, pageable,
                new LogQueryTask<>(ProjectOperationLog.class, projectLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    /**
     * 查询用户在指定项目的项目业务模块的操作日志
     */
    @Override
    public Page<ProjectOperationLog> getUserProjectLogsInProject(Long projectId, Long userId,
                                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                                 Pageable pageable) {
        return getUserModuleLogsInProject(projectId, userId, startTime, endTime, pageable,
                new LogQueryTask<>(ProjectOperationLog.class, projectLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    /**
     * 查询用户在所有项目的任务操作日志
     */
    @Override
    public Page<TaskOperationLog> getUserTaskLogs(Long userId, LocalDateTime startTime,
                                                  LocalDateTime endTime, Pageable pageable) {
        return getUserModuleLogsInProject(null, userId, startTime, endTime, pageable,
                new LogQueryTask<>(TaskOperationLog.class, taskLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    /**
     * 查询用户在指定项目的任务操作日志
     */
    @Override
    public Page<TaskOperationLog> getUserTaskLogsInProject(Long projectId, Long userId,
                                                           LocalDateTime startTime, LocalDateTime endTime,
                                                           Pageable pageable) {
        return getUserModuleLogsInProject(projectId, userId, startTime, endTime, pageable,
                new LogQueryTask<>(TaskOperationLog.class, taskLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    /**
     * 查询用户在所有项目的Wiki操作日志
     */
    @Override
    public Page<WikiOperationLog> getUserWikiLogs(Long userId, LocalDateTime startTime,
                                                  LocalDateTime endTime, Pageable pageable) {
        return getUserModuleLogsInProject(null, userId, startTime, endTime, pageable,
                new LogQueryTask<>(WikiOperationLog.class, wikiLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    /**
     * 查询用户在指定项目的Wiki操作日志
     */
    @Override
    public Page<WikiOperationLog> getUserWikiLogsInProject(Long projectId, Long userId,
                                                           LocalDateTime startTime, LocalDateTime endTime,
                                                           Pageable pageable) {
        return getUserModuleLogsInProject(projectId, userId, startTime, endTime, pageable,
                new LogQueryTask<>(WikiOperationLog.class, wikiLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    /**
     * 查询用户在所有项目的成果操作日志
     */
    @Override
    public Page<AchievementOperationLog> getUserAchievementLogs(Long userId, LocalDateTime startTime,
                                                                LocalDateTime endTime, Pageable pageable) {
        return getUserModuleLogsInProject(null, userId, startTime, endTime, pageable,
                new LogQueryTask<>(AchievementOperationLog.class, achievementLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    /**
     * 查询用户在指定项目的成果操作日志
     */
    @Override
    public Page<AchievementOperationLog> getUserAchievementLogsInProject(Long projectId, Long userId,
                                                                         LocalDateTime startTime, LocalDateTime endTime,
                                                                         Pageable pageable) {
        return getUserModuleLogsInProject(projectId, userId, startTime, endTime, pageable,
                new LogQueryTask<>(AchievementOperationLog.class, achievementLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD));
    }

    // ========== 私有核心方法 ==========
     /**
     * 构建所有日志类型的查询任务列表
     */
    private List<LogQueryTask<?>> buildAllLogQueryTasks() {
        return Arrays.asList(
                // 项目
                new LogQueryTask<>(ProjectOperationLog.class, projectLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD),
                // 任务
                new LogQueryTask<>(TaskOperationLog.class, taskLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD),
                // wiki
                new LogQueryTask<>(WikiOperationLog.class, wikiLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD),
                // 成果
                new LogQueryTask<>(AchievementOperationLog.class, achievementLogRepository,
                        operationLogConverter::toUnifiedVO, OPERATION_TIME_FIELD)
        );
    }

    /**
     * 通用方法：查询用户在指定/所有项目的指定模块日志（带时间范围筛选）
     * 合并原有的两个泛型查询方法，通过projectId是否为空控制范围
     */
    private <T> Page<T> getUserModuleLogsInProject(Long projectId, Long userId,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   Pageable pageable, LogQueryTask<T> task) {
        // 参数校验
        ValidationUtils.requireNonNull(userId, "用户ID不能为空");
        ValidationUtils.requireNonNull(pageable, "分页参数不能为空");
        if (projectId != null) {
            ValidationUtils.requireNonNull(projectId, "项目ID不能为空");
        }

        // 构建查询条件
        Specification<T> spec = buildQuerySpecification(projectId, userId, startTime, endTime, task.timeField());
        // 构建分页排序参数
        Pageable sortedPageable = buildSortedPageable(pageable, task.timeField());

        return task.repository().findAll(spec, sortedPageable);
    }

    /**
     * 构建通用查询条件（支持项目ID、用户ID、时间范围）
     */
    private <T> Specification<T> buildQuerySpecification(Long projectId, Long userId,
                                                         LocalDateTime startTime, LocalDateTime endTime,
                                                         String timeField) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 项目ID条件（可选）
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            // 用户ID条件（必选）
            predicates.add(cb.equal(root.get("userId"), userId));
            // 开始时间条件（可选）
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(timeField), startTime));
            }
            // 结束时间条件（可选）
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(timeField), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 轻量查询：根据项目ID（可选）和用户ID查询id和时间字段
     */
    private <T> List<LogIdTimeInfo> queryIdAndTime(LogQueryTask<T> task, Long projectId, Long userId) {
        Specification<T> spec = buildQuerySpecification(projectId, userId, null, null, task.timeField());
        List<T> entities = task.repository().findAll(spec, buildDefaultSort(task.timeField()));

        String source = getSourceByTask(task);
        return entities.stream()
                .map(entity -> new LogIdTimeInfo(extractId(entity), extractTime(entity, task.timeField()), source))
                .collect(Collectors.toList());
    }

    /**
     * 批量查询详情：根据id列表批量查询实体，避免N+1查询问题
     */
    @SuppressWarnings("unchecked")
    private <T> List<UnifiedOperationLogVO> batchQueryByIds(LogQueryTask<T> task, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询实体
        Iterable<T> entitiesIterable = ((CrudRepository<T, Long>) task.repository()).findAllById(ids);
        List<T> entities = new ArrayList<>();
        entitiesIterable.forEach(entities::add);

        // 构建ID-实体映射，保持顺序
        Map<Long, T> entityMap = entities.stream()
                .collect(Collectors.toMap(this::extractId, Function.identity()));

        // 转换为VO并保持原顺序
        return ids.stream()
                .map(entityMap::get)
                .filter(Objects::nonNull)
                .map(task.mapper())
                .collect(Collectors.toList());
    }

    // ========== 工具方法 ==========
    /**
     * 构建带默认排序的分页参数
     */
    private Pageable buildSortedPageable(Pageable pageable, String sortField) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                buildDefaultSort(sortField)
        );
    }

    /**
     * 构建默认排序（时间倒序）
     */
    private Sort buildDefaultSort(String sortField) {
        return Sort.by(DEFAULT_SORT_DIRECTION, sortField);
    }

    /**
     * 提取实体ID（使用反射）
     */
    private <T> Long extractId(T entity) {
        try {
            return (Long) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            log.error("提取实体ID失败: {}", entity.getClass().getName(), e);
            throw new ServiceException("提取实体ID失败", e);
        }
    }

    /**
     * 提取实体时间字段（使用反射）
     */
    private <T> LocalDateTime extractTime(T entity, String timeField) {
        try {
            String methodName = "get" + Character.toUpperCase(timeField.charAt(0)) + timeField.substring(1);
            return (LocalDateTime) entity.getClass().getMethod(methodName).invoke(entity);
        } catch (Exception e) {
            log.error("提取实体时间字段失败: {}, 字段: {}", entity.getClass().getName(), timeField, e);
            throw new ServiceException("提取实体时间字段失败", e);
        }
    }

    /**
     * 根据任务获取来源标识
     */
    private String getSourceByTask(LogQueryTask<?> task) {
        if (task.entityClass() == ProjectOperationLog.class) {
            return "PROJECT";
        } else if (task.entityClass() == TaskOperationLog.class) {
            return "TASK";
        } else if (task.entityClass() == WikiOperationLog.class) {
            return "WIKI";
        } else if (task.entityClass() == AchievementOperationLog.class) {
            return "ACHIEVEMENT";
        }
        throw new ServiceException("未知的实体类型: " + task.entityClass().getName());
    }
}