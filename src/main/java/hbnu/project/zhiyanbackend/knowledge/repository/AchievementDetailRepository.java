package hbnu.project.zhiyanbackend.knowledge.repository;

import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementDetail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 成果详情数据访问层
 * 处理成果详细信息的存储和查询，包括自定义字段的JSON数据
 *
 * @author ErgouTree
 */
@Repository
public interface AchievementDetailRepository extends JpaRepository<AchievementDetail, Long>, JpaSpecificationExecutor<AchievementDetail> {

    /**
     * 根据成果ID查询详情，用于前端展示
     *
     * @param achievementId 成果ID
     * @return 成果详情
     */
    Optional<AchievementDetail> findByAchievementId(Long achievementId);

    /**
     * 检查成果是否存在其详情信息，用于避免悬空成果
     *
     * @param achievementId 成果ID
     * @return 是否存在
     */
    boolean existsByAchievementId(Long achievementId);

    /**
     * 删除成果详情
     * @param achievementId 成果ID
     * @return 删除的记录数
     */
    @Modifying
    @Transactional
    int deleteByAchievementId(Long achievementId);

    /**
     * 批量查询成果详情
     *
     * @param achievementIds 成果ID列表
     * @return 成果详情列表
     */
    @Query("SELECT ad FROM AchievementDetail ad WHERE ad.achievementId IN :achievementIds")
    List<AchievementDetail> findByAchievementIdIn(@Param("achievementIds") List<Long> achievementIds);

    /**
     * 根据JSON字段是否存在查询成就详情记录
     *
     * 使用PostgreSQL的jsonb_exists函数检查detailData字段中是否包含指定的JSON属性
     * 适用于需要筛选包含特定JSON属性的记录场景
     *
     * @param field 要检查的JSON属性名称（例如："score"、"level"）
     * @return 包含指定JSON属性的成就详情记录列表
     */
    @Query("SELECT ad FROM AchievementDetail ad WHERE FUNCTION('jsonb_exists', ad.detailData, :field) = true")
    List<AchievementDetail> findByJsonFieldExists(@Param("field") String field);

    /**
     * 根据JSON字段的具体值查询成就详情记录
     *
     * 使用PostgreSQL的jsonb_extract_path_text函数提取detailData字段中指定属性的值，
     * 并与给定值进行精确匹配查询
     * 适用于需要根据JSON属性值进行筛选的场景
     *
     * @param field 要查询的JSON属性名称
     * @param value 要匹配的JSON属性值
     * @return JSON属性值匹配的成就详情记录列表
     */
    @Query("SELECT ad FROM AchievementDetail ad WHERE FUNCTION('jsonb_extract_path_text', ad.detailData, :field) = :value")
    List<AchievementDetail> findByJsonFieldValue(@Param("field") String field, @Param("value") String value);
}

