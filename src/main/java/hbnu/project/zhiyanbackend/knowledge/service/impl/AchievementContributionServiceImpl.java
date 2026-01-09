package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.activelog.model.entity.AchievementOperationLog;
import hbnu.project.zhiyanbackend.activelog.repository.AchievementOperationLogRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementContributionDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementContributionDetailDTO;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成果贡献统计服务实现类
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementContributionServiceImpl implements AchievementContributionService {

    private final AchievementOperationLogRepository achievementOperationLogRepository;
    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;

    /**
     * 获取项目成果贡献统计数据
     * 用于生成贡献热力图
     *
     * @param projectId 项目ID
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     * @return 贡献统计数据列表，每个元素包含日期和贡献数
     */
    @Override
    public List<AchievementContributionDTO> getContributions(Long projectId, LocalDate startDate, LocalDate endDate) {
        log.info("获取项目成果贡献统计: projectId={}, startDate={}, endDate={}", projectId, startDate, endDate);

        // 转换为LocalDateTime（开始时间设为00:00:00，结束时间设为23:59:59）
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        // 查询数据库
        List<Object[]> results = achievementOperationLogRepository.countContributionsByDate(
                projectId, startTime, endTime);

        // 转换为DTO列表
        List<AchievementContributionDTO> contributions = new ArrayList<>();
        for (Object[] result : results) {
            // MySQL的DATE()函数返回java.sql.Date，需要转换为字符串
            Object dateObj = result[0];
            String date;
            if (dateObj instanceof java.sql.Date) {
                date = ((java.sql.Date) dateObj).toLocalDate().toString();
            } else if (dateObj instanceof java.time.LocalDate) {
                date = ((java.time.LocalDate) dateObj).toString();
            } else if (dateObj instanceof String) {
                date = (String) dateObj;
            } else {
                // 尝试转换为字符串
                date = dateObj.toString();
            }

            Long count = ((Number) result[1]).longValue();
            contributions.add(AchievementContributionDTO.builder()
                    .date(date)
                    .count(count)
                    .build());
        }

        log.info("贡献统计查询完成: projectId={}, 返回记录数={}", projectId, contributions.size());
        return contributions;
    }

    /**
     * 获取项目成果贡献统计数据（默认最近一年）
     *
     * @param projectId 项目ID
     * @return 贡献统计数据列表
     */
    @Override
    public List<AchievementContributionDTO> getContributionsLastYear(Long projectId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        return getContributions(projectId, startDate, endDate);
    }

    /**
     * 获取项目成果详细贡献数据
     *
     * @param projectId 项目ID
     * @param date      日期（格式：yyyy-MM-dd）
     * @return 详细贡献数据
     */
    @Override
    public AchievementContributionDetailDTO getContributionDetails(Long projectId, LocalDate date) {
        log.info("获取项目成果详细贡献数据: projectId={}, date={}", projectId, date);

        // 转换为LocalDateTime（开始时间设为00:00:00，结束时间设为23:59:59）
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);

        // 查询该日期所有的成果操作日志（使用大分页获取所有记录）
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        List<AchievementOperationLog> logs = achievementOperationLogRepository
                .findByProjectIdAndOperationTimeBetweenOrderByOperationTimeDesc(projectId, startTime, endTime, pageable)
                .getContent();

        // 按用户ID分组统计
        Map<Long, List<AchievementOperationLog>> logsByUser = logs.stream()
                .collect(Collectors.groupingBy(AchievementOperationLog::getUserId));

        // 获取所有涉及的成果ID和用户ID
        Set<Long> achievementIds = logs.stream()
                .map(AchievementOperationLog::getAchievementId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> userIds = logsByUser.keySet();

        // 批量查询成果信息（如果成果存在）
        Map<Long, String> achievementTitleMap = achievementRepository.findAllById(achievementIds).stream()
                .collect(Collectors.toMap(
                        achievement -> achievement.getId(),
                        achievement -> achievement.getTitle() != null ? achievement.getTitle() : "未命名成果"
                ));

        // 批量查询用户信息
        Map<Long, String> userNameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getName() != null ? user.getName() : (user.getEmail() != null ? user.getEmail() : "未知用户")
                ));

        // 构建贡献者详细信息列表
        List<AchievementContributionDetailDTO.ContributorDetail> contributors = new ArrayList<>();
        for (Map.Entry<Long, List<AchievementOperationLog>> entry : logsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<AchievementOperationLog> userLogs = entry.getValue();

            // 获取该用户贡献的成果列表（去重）
            Map<Long, AchievementOperationLog> achievementLogMap = userLogs.stream()
                    .filter(log -> log.getAchievementId() != null)
                    .collect(Collectors.toMap(
                            AchievementOperationLog::getAchievementId,
                            log -> log,
                            (existing, replacement) -> existing // 如果有重复，保留第一个
                    ));

            List<AchievementContributionDetailDTO.AchievementInfo> achievements = achievementLogMap.values().stream()
                    .map(log -> {
                        String title = achievementTitleMap.getOrDefault(log.getAchievementId(), "未命名成果");
                        // 获取操作类型的中文描述
                        String operationTypeDesc = "未知";
                        if (log.getOperationType() != null) {
                            operationTypeDesc = log.getOperationType().getDesc();
                        }
                        return AchievementContributionDetailDTO.AchievementInfo.builder()
                                .id(log.getAchievementId())
                                .title(title)
                                .name(title) // 兼容字段
                                .type(operationTypeDesc) // 使用中文描述
                                .createdAt(log.getOperationTime() != null ? log.getOperationTime().toString() : "")
                                .time(log.getOperationTime() != null ? log.getOperationTime().toString() : "") // 兼容字段
                                .build();
                    })
                    .collect(Collectors.toList());

            String username = userNameMap.getOrDefault(userId, "未知用户");
            contributors.add(AchievementContributionDetailDTO.ContributorDetail.builder()
                    .userId(userId)
                    .username(username)
                    .name(username) // 兼容字段
                    .count((long) userLogs.size())
                    .achievements(achievements)
                    .build());
        }

        // 按贡献次数降序排序
        contributors.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        // 构建返回结果
        AchievementContributionDetailDTO result = AchievementContributionDetailDTO.builder()
                .date(date.toString())
                .totalCount((long) logs.size())
                .contributors(contributors)
                .build();

        log.info("详细贡献数据查询完成: projectId={}, date={}, 总贡献数={}, 贡献者数={}",
                projectId, date, result.getTotalCount(), contributors.size());
        return result;
    }

    /**
     * 用于周视图的成果贡献方法
     */
    @Override
    public AchievementContributionDetailDTO getContributionDetails(Long projectId, LocalDate startDate, LocalDate endDate) {
        log.info("获取项目成果详细贡献数据（日期范围）: projectId={}, startDate={}, endDate={}", projectId, startDate, endDate);

        // 转换为LocalDateTime（开始时间设为00:00:00，结束时间设为23:59:59）
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        // 查询该日期范围所有的成果操作日志（使用大分页获取所有记录）
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        List<AchievementOperationLog> logs = achievementOperationLogRepository
                .findByProjectIdAndOperationTimeBetweenOrderByOperationTimeDesc(projectId, startTime, endTime, pageable)
                .getContent();

        // 按用户ID分组统计
        Map<Long, List<AchievementOperationLog>> logsByUser = logs.stream()
                .collect(Collectors.groupingBy(AchievementOperationLog::getUserId));

        // 获取所有涉及的成果ID和用户ID
        Set<Long> achievementIds = logs.stream()
                .map(AchievementOperationLog::getAchievementId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> userIds = logsByUser.keySet();

        // 批量查询成果信息（如果成果存在）
        Map<Long, String> achievementTitleMap = achievementRepository.findAllById(achievementIds).stream()
                .collect(Collectors.toMap(
                        achievement -> achievement.getId(),
                        achievement -> achievement.getTitle() != null ? achievement.getTitle() : "未命名成果"
                ));

        // 批量查询用户信息
        Map<Long, String> userNameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getName() != null ? user.getName() : (user.getEmail() != null ? user.getEmail() : "未知用户")
                ));

        // 构建贡献者详细信息列表
        List<AchievementContributionDetailDTO.ContributorDetail> contributors = new ArrayList<>();
        for (Map.Entry<Long, List<AchievementOperationLog>> entry : logsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<AchievementOperationLog> userLogs = entry.getValue();

            // 获取该用户贡献的成果列表（去重）
            Map<Long, AchievementOperationLog> achievementLogMap = userLogs.stream()
                    .filter(log -> log.getAchievementId() != null)
                    .collect(Collectors.toMap(
                            AchievementOperationLog::getAchievementId,
                            log -> log,
                            (existing, replacement) -> existing // 如果有重复，保留第一个
                    ));

            List<AchievementContributionDetailDTO.AchievementInfo> achievements = achievementLogMap.values().stream()
                    .map(log -> {
                        String title = achievementTitleMap.getOrDefault(log.getAchievementId(), "未命名成果");
                        // 获取操作类型的中文描述
                        String operationTypeDesc = "未知";
                        if (log.getOperationType() != null) {
                            operationTypeDesc = log.getOperationType().getDesc();
                        }
                        return AchievementContributionDetailDTO.AchievementInfo.builder()
                                .id(log.getAchievementId())
                                .title(title)
                                .name(title) // 兼容字段
                                .type(operationTypeDesc) // 使用中文描述
                                .createdAt(log.getOperationTime() != null ? log.getOperationTime().toString() : "")
                                .time(log.getOperationTime() != null ? log.getOperationTime().toString() : "") // 兼容字段
                                .build();
                    })
                    .collect(Collectors.toList());

            String username = userNameMap.getOrDefault(userId, "未知用户");
            contributors.add(AchievementContributionDetailDTO.ContributorDetail.builder()
                    .userId(userId)
                    .username(username)
                    .name(username) // 兼容字段
                    .count((long) userLogs.size())
                    .achievements(achievements)
                    .build());
        }

        // 按贡献次数降序排序
        contributors.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        // 构建返回结果，日期范围格式：yyyy-MM-dd - yyyy-MM-dd
        String dateRange = startDate.toString() + " - " + endDate.toString();
        AchievementContributionDetailDTO result = AchievementContributionDetailDTO.builder()
                .date(dateRange)
                .totalCount((long) logs.size())
                .contributors(contributors)
                .build();

        log.info("详细贡献数据查询完成（日期范围）: projectId={}, startDate={}, endDate={}, 总贡献数={}, 贡献者数={}",
                projectId, startDate, endDate, result.getTotalCount(), contributors.size());
        return result;
    }
}

