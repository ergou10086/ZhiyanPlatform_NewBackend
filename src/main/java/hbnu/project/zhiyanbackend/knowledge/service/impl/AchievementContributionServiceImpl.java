package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.activelog.repository.AchievementOperationLogRepository;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementContributionDTO;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Override
    public List<AchievementContributionDTO> getContributionsLastYear(Long projectId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        return getContributions(projectId, startDate, endDate);
    }
}

