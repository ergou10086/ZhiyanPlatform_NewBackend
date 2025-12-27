package hbnu.project.zhiyanbackend.knowledge.service;

import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementContributionDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 成果贡献统计服务接口
 * 提供成果贡献热力图所需的数据统计功能
 *
 * @author ErgouTree
 */
public interface AchievementContributionService {

    /**
     * 获取项目成果贡献统计数据
     * 用于生成贡献热力图
     *
     * @param projectId 项目ID
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     * @return 贡献统计数据列表，每个元素包含日期和贡献数
     */
    List<AchievementContributionDTO> getContributions(Long projectId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取项目成果贡献统计数据（默认最近一年）
     *
     * @param projectId 项目ID
     * @return 贡献统计数据列表
     */
    List<AchievementContributionDTO> getContributionsLastYear(Long projectId);
}

