package hbnu.project.zhiyanbackend.activelog.service.impl;

import hbnu.project.zhiyanbackend.activelog.model.converter.OperationLogConverter;
import hbnu.project.zhiyanbackend.activelog.model.dto.ProjectLogExportDTO;
import hbnu.project.zhiyanbackend.activelog.model.dto.UnifiedLogExportDTO;
import hbnu.project.zhiyanbackend.activelog.model.entity.*;
import hbnu.project.zhiyanbackend.activelog.model.vo.UnifiedOperationLogVO;
import hbnu.project.zhiyanbackend.activelog.repository.*;
import hbnu.project.zhiyanbackend.activelog.service.OperationLogExportService;
import hbnu.project.zhiyanbackend.activelog.service.OperationLogService;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 操作日志导出服务实现
 * 使用hutool代替之前的fastexcel
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogExportServiceImpl implements OperationLogExportService {

    private final ProjectOperationLogRepository projectLogRepository;
    private final TaskOperationLogRepository taskLogRepository;
    private final WikiOperationLogRepository wikiLogRepository;
    private final AchievementOperationLogRepository achievementLogRepository;
    private final OperationLogConverter operationLogConverter;
    private final OperationLogService operationLogService;

    /**
     * 默认最大查询条数，防止全表扫描
     */
    private static final int DEFAULT_MAX_QUERY_LIMIT = 10000;

    /**
     * 操作时间字段名
     */
    private static final String OPERATION_TIME_FIELD = "operationTime";

    /**
     * 日志查询任务定义
     * 封装了实体类型、Repository、Mapper转换函数和时间字段名
     */
    private record LogQueryTask<T>(
            Class<T> entityClass,
            JpaSpecificationExecutor<T> repository,
            Function<T, UnifiedOperationLogVO> mapper,
            String timeField
    ) {}

    /**
     * 导出项目的全部操作日志
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
    @Override
    public void exportProjectLogs(Long projectId, String operationType, String username, LocalDateTime startTime, LocalDateTime endTime, Integer limit, HttpServletResponse response) {
        ValidationUtils.requireNonNull(projectId, "项目ID不能为空");
        ValidationUtils.requireNonNull(response, "HTTP响应不能为空");

        try{
            // 应用查询限制
            int effectiveLimit = applyQueryLimit(limit);

            // 构建分页参数
            Sort sort = Sort.by(Sort.Direction.DESC, OPERATION_TIME_FIELD);
            Pageable pageable = PageRequest.of(0, effectiveLimit, sort);

            // 查询日志
            Page<ProjectOperationLog> page = operationLogService.getProjectLogs(
                    projectId, operationType, username, startTime, endTime, pageable);
            List<ProjectOperationLog> logs = page.getContent();

            // 转换为导出DTO
            List<ProjectLogExportDTO> exportList = operationLogConverter.toProjectExportDTOList(logs);

            // 导出Excel
            exportToExcel(exportList, "项目操作日志", ProjectLogExportDTO.class, response);

            log.info("导出项目操作日志成功，项目ID: {}, 条数: {}", projectId, exportList.size());
        }catch (Exception e){
            log.error("导出项目操作日志失败，项目ID: {}", projectId, e);
            throw new ServiceException("导出项目操作日志失败: " + e.getMessage(), e);
        }
    }

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
    @Override
    public void exportMyLogs(Long userId, LocalDateTime startTime, LocalDateTime endTime, Integer limit, HttpServletResponse response) {
        ValidationUtils.requireNonNull(userId, "用户ID不能为空");
        ValidationUtils.requireNonNull(response, "HTTP响应不能为空");

        try {
            // 定义所有需要查询的日志类型（新架构没有登录日志）
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

            // 应用查询限制
            int effectiveLimit = applyQueryLimit(limit);

            // 使用泛型方法查询所有类型的日志
            List<List<UnifiedOperationLogVO>> allLogLists = queryTasks.stream()
                    .map(task -> queryLogsByUserAndTime(task, userId, startTime, endTime, effectiveLimit))
                    .filter(list -> !list.isEmpty())
                    .collect(Collectors.toList());

            // 使用多路归并算法合并已排序的列表
            List<UnifiedOperationLogVO> mergedLogs = mergeSortedLogs(allLogLists, effectiveLimit);

            // 转换为导出DTO
            List<UnifiedLogExportDTO> exportList = operationLogConverter.toUnifiedExportDTOList(mergedLogs);

            // 导出Excel
            exportToExcel(exportList, "我的操作日志", UnifiedLogExportDTO.class, response);

            log.info("导出我的操作日志成功，用户ID: {}, 条数: {}", userId, exportList.size());
        } catch (Exception e) {
            log.error("导出我的操作日志失败，用户ID: {}", userId, e);
            throw new ServiceException("导出我的操作日志失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导出我在指定项目内的所有操作日志
     *
     * @param projectId 项目ID（必填）
     * @param userId    用户ID（必填）
     * @param limit     导出条数限制，null表示不限制（但不超过默认最大值）
     * @param response  HTTP响应
     */
    @Override
    public void exportMyProjectLogs(Long projectId, Long userId, Integer limit, HttpServletResponse response) {
        ValidationUtils.requireNonNull(projectId, "项目ID不能为空");
        ValidationUtils.requireNonNull(userId, "用户ID不能为空");
        ValidationUtils.requireNonNull(response, "HTTP响应不能为空");

        try{
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

            // 应用查询限制
            int effectiveLimit = applyQueryLimit(limit);

            // 使用泛型方法查询所有类型的日志
            List<List<UnifiedOperationLogVO>> allLogLists = queryTasks.stream()
                    .map(task -> queryLogsByUserAndProject(task, userId, projectId, effectiveLimit))
                    .filter(list -> !list.isEmpty())
                    .collect(Collectors.toList());

            // 使用多路归并算法合并已排序的列表
            List<UnifiedOperationLogVO> mergedLogs = mergeSortedLogs(allLogLists, effectiveLimit);

            // 转换为导出DTO
            List<UnifiedLogExportDTO> exportList = operationLogConverter.toUnifiedExportDTOList(mergedLogs);

            // 导出Excel
            exportToExcel(exportList, "项目内我的操作日志", UnifiedLogExportDTO.class, response);

            log.info("导出项目内我的操作日志成功，用户ID: {}, 项目ID: {}, 条数: {}", userId, projectId, exportList.size());
        }catch (Exception e){
            log.error("导出项目内我的操作日志失败，用户ID: {}, 项目ID: {}", userId, projectId, e);
            throw new ServiceException("导出项目内我的操作日志失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 Hutool 导出 Excel
     *
     * @param data      导出数据列表
     * @param sheetName 工作表名称
     * @param clazz     数据类类型
     * @param response  HTTP响应
     * @param <T>       数据类型
     */
    private <T> void exportToExcel(List<T> data, String sheetName, Class<T> clazz, HttpServletResponse response) {
        try {
            // 设置响应头
            String fileName = URLEncoder.encode(sheetName + "_" + System.currentTimeMillis() + ".xlsx", StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.setCharacterEncoding("UTF-8");

            // 使用 Hutool 创建 ExcelWriter
            ExcelWriter writer = ExcelUtil.getWriter(true);

            // 设置工作表名称
            writer.renameSheet(sheetName);

            // 写入数据（Hutool 会自动根据字段生成表头）
            writer.write(data, true);

            // 自动调整列宽
            writer.autoSizeColumnAll();

            // 写入到响应流
            writer.flush(response.getOutputStream(), true);

            // 关闭 writer
            writer.close();

            log.debug("Excel导出成功，工作表: {}, 数据条数: {}", sheetName, data.size());
        } catch (IOException e) {
            log.error("Excel导出失败，工作表: {}", sheetName, e);
            throw new ServiceException("Excel导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通用方法：根据用户ID和时间范围查询日志
     * 使用泛型消除重复代码
     */
    @SuppressWarnings("unchecked")
    private <T> List<UnifiedOperationLogVO> queryLogsByUserAndTime(LogQueryTask<T> task, Long userId,
                                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                                   int limit) {
        Specification<T> spec = buildUserTimeSpec(task.entityClass, userId, startTime, endTime, task.timeField);
        Sort sort = Sort.by(Sort.Direction.DESC, task.timeField);
        Pageable pageable = PageRequest.of(0, limit, sort);

        Page<T> page = task.repository.findAll(spec, pageable);
        return page.getContent().stream()
                .map(task.mapper)
                .collect(Collectors.toList());
    }

    /**
     * 通用方法：根据用户ID和项目ID查询日志
     * 使用泛型消除重复代码
     */
    @SuppressWarnings("unchecked")
    private <T> List<UnifiedOperationLogVO> queryLogsByUserAndProject(LogQueryTask<T> task, Long userId,
                                                                      Long projectId, int limit) {
        Specification<T> spec = buildUserProjectSpec(task.entityClass, userId, projectId, task.timeField);
        Sort sort = Sort.by(Sort.Direction.DESC, task.timeField);
        Pageable pageable = PageRequest.of(0, limit, sort);

        Page<T> page = task.repository.findAll(spec, pageable);
        return page.getContent().stream()
                .map(task.mapper)
                .collect(Collectors.toList());
    }

    /**
     * 构建用户和时间范围的查询条件（泛型方法）
     */
    private <T> Specification<T> buildUserTimeSpec(Class<T> clazz, Long userId,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   String timeField) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

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
     * 构建用户和项目的查询条件（泛型方法）
     */
    private <T> Specification<T> buildUserProjectSpec(Class<T> clazz, Long userId, Long projectId, String timeField) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("projectId"), projectId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 多路归并算法：合并多个已排序的日志列表
     * 由于每个列表已经按时间倒序排序，使用多路归并可以高效合并
     * 时间复杂度：O(n * log(k))，其中n是总元素数，k是列表数量
     *
     * @param sortedLists 已排序的日志列表集合
     * @param limit       最大返回条数
     * @return 合并后的排序列表
     */
    private List<UnifiedOperationLogVO> mergeSortedLogs(List<List<UnifiedOperationLogVO>> sortedLists, int limit) {
        if (CollUtil.isEmpty(sortedLists)) {
            return Collections.emptyList();
        }

        // 如果只有一个列表，直接返回（截取到limit）
        if (sortedLists.size() == 1) {
            List<UnifiedOperationLogVO> singleList = sortedLists.getFirst();
            return singleList.size() > limit ? singleList.subList(0, limit) : singleList;
        }

        // 使用优先队列（最小堆）实现多路归并
        // 由于需要按时间倒序（最新的在前），所以使用时间最大的优先
        PriorityQueue<LogIterator> heap = new PriorityQueue<>(
                (a, b) -> b.current().getTime().compareTo(a.current().getTime())
        );

        // 初始化：将每个列表的第一个元素加入堆
        for (List<UnifiedOperationLogVO> list : sortedLists) {
            if (!list.isEmpty()) {
                heap.offer(new LogIterator(list));
            }
        }

        List<UnifiedOperationLogVO> result = new ArrayList<>(Math.min(limit, sortedLists.stream()
                .mapToInt(List::size)
                .sum()));

        // 归并过程
        while (!heap.isEmpty() && result.size() < limit) {
            LogIterator iterator = heap.poll();
            result.add(iterator.current());

            // 如果该迭代器还有下一个元素，继续加入堆
            if (iterator.hasNext()) {
                iterator.next();
                heap.offer(iterator);
            }
        }

        return result;
    }

    /**
     * 日志列表迭代器包装类
     * 用于多路归并算法
     */
    private static class LogIterator {
        private final List<UnifiedOperationLogVO> list;
        private int index;

        LogIterator(List<UnifiedOperationLogVO> list) {
            this.list = list;
            this.index = 0;
        }

        UnifiedOperationLogVO current() {
            return list.get(index);
        }

        boolean hasNext() {
            return index < list.size() - 1;
        }

        void next() {
            index++;
        }
    }

    /**
     * 应用查询限制
     * 如果limit为null或0，使用默认最大值；否则使用指定的limit，但不超过最大值
     *
     * @param limit 用户指定的限制
     * @return 有效的查询限制
     */
    private int applyQueryLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_MAX_QUERY_LIMIT;
        }
        return Math.min(limit, DEFAULT_MAX_QUERY_LIMIT);
    }
}
