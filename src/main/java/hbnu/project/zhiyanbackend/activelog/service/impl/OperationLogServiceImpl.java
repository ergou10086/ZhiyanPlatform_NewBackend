package hbnu.project.zhiyanbackend.activelog.service.impl;

import hbnu.project.zhiyanbackend.activelog.model.converter.OperationLogConverter;
import hbnu.project.zhiyanbackend.activelog.model.entity.*;
import hbnu.project.zhiyanbackend.activelog.model.enums.AchievementOperationType;
import hbnu.project.zhiyanbackend.activelog.model.enums.ProjectOperationType;
import hbnu.project.zhiyanbackend.activelog.model.enums.TaskOperationType;
import hbnu.project.zhiyanbackend.activelog.model.enums.WikiOperationType;
import hbnu.project.zhiyanbackend.activelog.model.vo.UnifiedOperationLogVO;
import hbnu.project.zhiyanbackend.activelog.repository.*;
import hbnu.project.zhiyanbackend.activelog.service.OperationLogService;
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
 * 操作日志服务实现类
 * <p>
 * 实现OperationLogService接口，提供各业务模块日志查询的具体实现，
 * 核心优化点：
 * 1. 泛型通用查询逻辑，减少重复代码
 * 2. 轻量查询+批量加载，避免N+1问题
 * 3. 跨表合并分页，保证日志时间序的一致性
 *
 * @author ErgouTree
 * @rewrite yui
 * @rewrite ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final ProjectOperationLogRepository projectLogRepository;
    private final TaskOperationLogRepository taskLogRepository;
    private final WikiOperationLogRepository wikiLogRepository;
    private final AchievementOperationLogRepository achievementLogRepository;
    private final OperationLogConverter operationLogConverter;

    /**
     * 操作时间字段名
     */
    private static final String OPERATION_TIME_FIELD = "operationTime";

    /**
     * 日志查询任务定义
     * 封装了实体类型、Repository、Mapper转换函数和时间字段名
     */
    public record LogQueryTask<T>(Class<T> entityClass, JpaSpecificationExecutor<T> repository,
                                      Function<T, UnifiedOperationLogVO> mapper, String timeField) {
    }

    /**
     * 日志ID和时间信息
     * 用于轻量查询，只查询主键和时间字段
     *
     * @param source 标识来源表（PROJECT/TASK/WIKI/ACHIEVEMENT）
     */
    private record LogIdTimeInfo(Long id, LocalDateTime time, String source) {}

    // ==================== 实现接口方法 ====================

    /**
     * 查询项目内所有类型的操作日志（分页）
     * 实现接口核心方法，具体逻辑见接口注释
     */
    @Override
    public Page<UnifiedOperationLogVO> getProjectAllLogs(Long projectId, Pageable pageable) {
        // 参数校验
        ValidationUtils.requireNonNull(projectId, "项目ID不能为空");
        ValidationUtils.requireNonNull(pageable, "分页参数不能为空");

        try{
            // 定义所有需要查询的日志类型
            List<LogQueryTask<?>> queryTasks = Arrays.asList(
                    new LogQueryTask<>(
                            ProjectOperationLog.class,
                            projectLogRepository,
                            operationLogConverter::toUnifiedVO,
                            OPERATION_TIME_FIELD
                    ),
                    new LogQueryTask<>(
                            TaskOperationLog.class,
                            taskLogRepository,
                            operationLogConverter::toUnifiedVO,
                            OPERATION_TIME_FIELD
                    ),
                    new LogQueryTask<>(
                            WikiOperationLog.class,
                            wikiLogRepository,
                            operationLogConverter::toUnifiedVO,
                            OPERATION_TIME_FIELD
                    ),
                    new LogQueryTask<>(
                            AchievementOperationLog.class,
                            achievementLogRepository,
                            operationLogConverter::toUnifiedVO,
                            OPERATION_TIME_FIELD
                    )
            );

            // 第一步：轻量查询 - 只查询各表的id和时间
            List<LogIdTimeInfo> allIdTimeInfos = new ArrayList<>();
            for (LogQueryTask<?> task : queryTasks) {
                List<LogIdTimeInfo> idTimeInfos = queryIdAndTimeByProject(task, projectId);
                allIdTimeInfos.addAll(idTimeInfos);
            }

            // 第二步：合并排序（按时间倒序）
            allIdTimeInfos.sort((a, b) -> b.time.compareTo(a.time));

            // 第三步：计算总数和分页的范围
            long total = allIdTimeInfos.size();
            int pageNumber = pageable.getPageNumber();
            int pageSize = pageable.getPageSize();
            int start = pageNumber * pageSize;
            int end = Math.min(start + pageSize, allIdTimeInfos.size());
            // 如果超出范围，返回空结果，此为保险
            if (start >= total) {
                return new PageImpl<>(Collections.emptyList(), pageable, total);
            }

            // 第四步：取分页范围内的id列表
            List<LogIdTimeInfo> pagedIdTimeInfos = allIdTimeInfos.subList(start, end);

            // 第五步：按来源分组，批量查询详情
            Map<String, List<Long>> idsBySource = pagedIdTimeInfos.stream()
                    .collect(Collectors.groupingBy(
                            info -> info.source,
                            Collectors.mapping(info -> info.id, Collectors.toList())
                    ));

            // 第六步：批量查询详情并映射
            List<UnifiedOperationLogVO> result = new ArrayList<>();
            for (LogQueryTask<?> task : queryTasks) {
                String source = getSourceByTask(task);
                List<Long> ids = idsBySource.get(source);
                if (ids != null && !ids.isEmpty()) {
                    List<UnifiedOperationLogVO> vos = batchQueryByIds(task, ids);
                    result.addAll(vos);
                }
            }

            // 第七步：再按时间倒序排序，因为批量查询后顺序被打乱
            result.sort((a, b) -> b.getTime().compareTo(a.getTime()));

            return new PageImpl<>(result, pageable, total);
        }catch (ServiceException e){
            log.error("查询项目内所有操作日志失败，项目ID: {}", projectId, e);
            throw new ServiceException("查询项目内所有操作日志失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通用方法：查询项目内指定类型的日志（带筛选）
     * 实现接口泛型方法，具体逻辑见接口注释
     */
    @Override
    public <T> Page<T> queryProjectLogsWithFilter(LogQueryTask<T> task, Long projectId,
                                                  String operationType, String username,
                                                  LocalDateTime startTime, LocalDateTime endTime,
                                                  Pageable pageable,
                                                  Function<Specification<T>, Specification<T>> extraPredicate) {
        // 参数校验
        ValidationUtils.requireNonNull(projectId, "项目ID不能为空");
        ValidationUtils.requireNonNull(pageable, "分页参数不能为空");

        Specification<T> baseSpec = buildBaseProjectLogSpec(task.entityClass, projectId, operationType,
                username, startTime, endTime, task.timeField);

        // 应用额外的查询条件
        Specification<T> finalSpec = extraPredicate != null ? extraPredicate.apply(baseSpec) : baseSpec;

        Sort sort = Sort.by(Sort.Direction.DESC, task.timeField);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return task.repository.findAll(finalSpec, sortedPageable);
    }

    /**
     * 查询项目内项目业务模块的的操作日志（带筛选）
     * 实现接口方法，具体逻辑见接口注释
     */
    @Override
    public Page<ProjectOperationLog> getProjectLogs(Long projectId, String operationType, String username,
                                                    LocalDateTime startTime, LocalDateTime endTime,
                                                    Pageable pageable) {
        LogQueryTask<ProjectOperationLog> task = new LogQueryTask<>(
                ProjectOperationLog.class,
                projectLogRepository,
                operationLogConverter::toUnifiedVO,
                OPERATION_TIME_FIELD
        );

        // 转换操作类型字符串为枚举
        ProjectOperationType operationTypeEnum = null;
        if (operationType != null && !operationType.isEmpty()) {
            try {
                operationTypeEnum = ProjectOperationType.valueOf(operationType);
            } catch (IllegalArgumentException e) {
                log.warn("无效的项目操作类型: {}", operationType);
            }
        }

        final ProjectOperationType finalOperationType = operationTypeEnum;
        Specification<ProjectOperationLog> spec = buildBaseProjectLogSpec(
                ProjectOperationLog.class, projectId, operationType, username, startTime, endTime, OPERATION_TIME_FIELD
        );

        // 如果指定了操作类型，添加操作类型条件
        if (finalOperationType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("operationType"), finalOperationType));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, OPERATION_TIME_FIELD);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return projectLogRepository.findAll(spec, sortedPageable);
    }

    /**
     * 查询项目内Wiki业务模块的操作日志（带筛选）
     * 实现接口方法，具体逻辑见接口注释
     */
    @Override
    public Page<WikiOperationLog> getWikiLogs(Long projectId, Long wikiPageId, String operationType,
                                              String username, LocalDateTime startTime, LocalDateTime endTime,
                                              Pageable pageable) {
        LogQueryTask<WikiOperationLog> task = new LogQueryTask<>(
                WikiOperationLog.class,
                wikiLogRepository,
                operationLogConverter::toUnifiedVO,
                OPERATION_TIME_FIELD
        );

        // 转换操作类型字符串为枚举
        WikiOperationType operationTypeEnum = null;
        if (operationType != null && !operationType.isEmpty()) {
            try {
                operationTypeEnum = WikiOperationType.valueOf(operationType);
            } catch (IllegalArgumentException e) {
                log.warn("无效的Wiki操作类型: {}", operationType);
            }
        }
        final WikiOperationType finalOperationType = operationTypeEnum;

        // 通过函数式接口传入额外的查询条件
        Function<Specification<WikiOperationLog>, Specification<WikiOperationLog>> extraPredicate = baseSpec ->
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(baseSpec.toPredicate(root, query, cb));

                    if(wikiPageId != null){
                        predicates.add(cb.equal(root.get("wikiPageId"), wikiPageId));
                    }

                    if (finalOperationType != null) {
                        predicates.add(cb.equal(root.get("operationType"), finalOperationType));
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                };

        return queryProjectLogsWithFilter(task, projectId, null, username, startTime, endTime,
                pageable, extraPredicate);
    }

    /**
     * 查询项目内成果业务模块的操作日志（带筛选）
     * 实现接口方法，具体逻辑见接口注释
     */
    @Override
    public Page<AchievementOperationLog> getAchievementLogs(Long projectId, Long achievementId,
                                                            String operationType, String username,
                                                            LocalDateTime startTime, LocalDateTime endTime,
                                                            Pageable pageable) {
        LogQueryTask<AchievementOperationLog> task = new LogQueryTask<>(
                AchievementOperationLog.class,
                achievementLogRepository,
                operationLogConverter::toUnifiedVO,
                OPERATION_TIME_FIELD
        );

        // 转换操作类型字符串为枚举
        AchievementOperationType operationTypeEnum = null;
        if (operationType != null && !operationType.isEmpty()) {
            try {
                operationTypeEnum = AchievementOperationType.valueOf(operationType);
            } catch (IllegalArgumentException e) {
                log.warn("无效的成果操作类型: {}", operationType);
            }
        }

        final AchievementOperationType finalOperationType = operationTypeEnum;

        // 通过函数式接口传入额外的查询条件（achievementId）
        Function<Specification<AchievementOperationLog>, Specification<AchievementOperationLog>> extraPredicate = baseSpec ->
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(baseSpec.toPredicate(root, query, cb));

                    if (achievementId != null) {
                        predicates.add(cb.equal(root.get("achievementId"), achievementId));
                    }

                    if (finalOperationType != null) {
                        predicates.add(cb.equal(root.get("operationType"), finalOperationType));
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                };

        return queryProjectLogsWithFilter(task, projectId, null, username, startTime, endTime,
                pageable, extraPredicate);
    }

    /**
     * 查询项目内任务业务模块的操作日志（带筛选）
     * 实现接口方法，具体逻辑见接口注释
     */
    @Override
    public Page<TaskOperationLog> getTaskLogs(Long projectId, Long taskId, String operationType,
                                              String username, LocalDateTime startTime, LocalDateTime endTime,
                                              Pageable pageable) {
        LogQueryTask<TaskOperationLog> task = new LogQueryTask<>(
                TaskOperationLog.class,
                taskLogRepository,
                operationLogConverter::toUnifiedVO,
                OPERATION_TIME_FIELD
        );

        // 转换操作类型字符串为枚举（兼容大小写，增强容错）
        TaskOperationType operationTypeEnum = null;
        if (operationType != null && !operationType.isEmpty()) {
            try {
                // 统一转大写后匹配枚举（避免前端传小写导致的匹配失败）
                operationTypeEnum = TaskOperationType.valueOf(operationType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("无效的任务操作类型: {}, 忽略该筛选条件", operationType);
            }
        }
        final TaskOperationType finalOperationType = operationTypeEnum;

        // 通过函数式接口传入额外的查询条件（taskId + 操作类型）
        Function<Specification<TaskOperationLog>, Specification<TaskOperationLog>> extraPredicate = baseSpec ->
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    predicates.add(baseSpec.toPredicate(root, query, cb));

                    // 任务ID筛选
                    if (taskId != null) {
                        predicates.add(cb.equal(root.get("taskId"), taskId));
                    }

                    // 操作类型筛选（枚举）
                    if (finalOperationType != null) {
                        predicates.add(cb.equal(root.get("operationType"), finalOperationType));
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                };

        // 调用通用筛选方法完成查询
        return queryProjectLogsWithFilter(task, projectId, null, username, startTime, endTime,
                pageable, extraPredicate);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建基础的项目日志查询条件（泛型方法）
     * 包含：项目ID + 操作类型 + 用户名 + 时间范围
     */
    private <T> Specification<T> buildBaseProjectLogSpec(Class<T> clazz, Long projectId, String operationType,
                                                         String username, LocalDateTime startTime,
                                                         LocalDateTime endTime, String timeField) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));

            if (username != null && !username.isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }

            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(timeField), startTime));
            }

            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(timeField), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 轻量查询：根据项目ID查询id和时间字段
     * 用于多表合并分页的场景
     */
    private <T> List<LogIdTimeInfo> queryIdAndTimeByProject(LogQueryTask<T> task, Long projectId) {
        Specification<T> spec = (root, query, cb) -> cb.equal(root.get("projectId"), projectId);

        // 只查询id和时间字段，减少数据传输
        // 注意：虽然JPA Specification默认查询所有字段，但这里我们只提取id和时间。
        // 如果数据量特别大（百万级），可以考虑使用原生SQL查询优化，只查询id和时间字段。
        List<T> entities = task.repository.findAll(spec, Sort.by(Sort.Direction.DESC, task.timeField));

        String source = getSourceByTask(task);
        return entities.stream()
                .map(entity -> {
                    Long id = extractId(entity);
                    LocalDateTime time = extractTime(entity, task.timeField);
                    return new LogIdTimeInfo(id, time, source);
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量查询详情：根据id列表批量查询实体
     * 避免N+1查询问题
     */
    @SuppressWarnings("unchecked")
    private <T> List<UnifiedOperationLogVO> batchQueryByIds(LogQueryTask<T> task, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用findAllById批量查询，避免N+1问题
        Iterable<T> entitiesIterable = ((CrudRepository<T, Long>) task.repository).findAllById(ids);
        List<T> entities = new ArrayList<>();
        entitiesIterable.forEach(entities::add);

        // 保持原有顺序（按ids的顺序）
        Map<Long, T> entityMap = entities.stream()
                .collect(Collectors.toMap(this::extractId, Function.identity()));

        return ids.stream()
                .map(entityMap::get)
                .filter(Objects::nonNull)
                .map(task.mapper)
                .collect(Collectors.toList());
    }

    /**
     * 提取实体ID（使用反射）
     */
    private <T> Long extractId(T entity) {
        try {
            var method = entity.getClass().getMethod("getId");
            return (Long) method.invoke(entity);
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
            // 根据字段名获取getter方法
            String methodName = "get" + timeField.substring(0, 1).toUpperCase() + timeField.substring(1);
            var method = entity.getClass().getMethod(methodName);
            return (LocalDateTime) method.invoke(entity);
        } catch (Exception e) {
            log.error("提取实体时间字段失败: {}, 字段: {}", entity.getClass().getName(), timeField, e);
            throw new ServiceException("提取实体时间字段失败", e);
        }
    }

    /**
     * 根据任务获取来源标识
     */
    private String getSourceByTask(LogQueryTask<?> task) {
        if (task.entityClass == ProjectOperationLog.class) {
            return "PROJECT";
        } else if (task.entityClass == TaskOperationLog.class) {
            return "TASK";
        } else if (task.entityClass == WikiOperationLog.class) {
            return "WIKI";
        } else if (task.entityClass == AchievementOperationLog.class) {
            return "ACHIEVEMENT";
        }
        throw new ServiceException("未知的实体类型: " + task.entityClass.getName());
    }
}