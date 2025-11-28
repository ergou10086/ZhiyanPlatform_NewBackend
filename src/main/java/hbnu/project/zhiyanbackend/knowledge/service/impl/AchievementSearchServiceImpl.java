package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementQueryDTO;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 成果搜索部分的服务实现
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementSearchServiceImpl implements AchievementSearchService {

    /**
     * 分页查询成果列表
     *
     * @param queryDTO 查询条件
     * @param pageable 分页参数
     * @return 成果分页列表
     */
    @Override
    public Page<AchievementDTO> queryAchievements(AchievementQueryDTO queryDTO, Pageable pageable) {
        return null;
    }

    /**
     * 根据项目ID查询成果列表
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 成果分页列表
     */
    @Override
    public Page<AchievementDTO> getAchievementsByProjectId(Long projectId, Pageable pageable) {
        return null;
    }

    /**
     * 根据成果名模糊查询成果
     *
     * @param achievementName 成果名
     * @return 成果
     */
    @Override
    public AchievementDTO getAchievementByName(String achievementName) {
        return null;
    }

    /**
     * 根据成果中的文件名模糊查询文件
     *
     * @param achievementFileName 文件名
     * @return 文件
     */
    @Override
    public AchievementFileDTO getAchievementFileByName(String achievementFileName) {
        return null;
    }

    /**
     * 统计查询：按类型统计成果数量
     *
     * @param projectId 项目ID
     * @return 统计结果Map
     */
    @Override
    public Map<String, Long> statisticsByType(Long projectId) {
        return Map.of();
    }
}
