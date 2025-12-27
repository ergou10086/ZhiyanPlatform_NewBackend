package hbnu.project.zhiyanbackend.activelog.repository;

import hbnu.project.zhiyanbackend.activelog.model.entity.AchievementOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.enums.AchievementOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成果操作日志Repository
 *
 * @author ErgouTree
 */
@Repository
public interface AchievementOperationLogRepository extends JpaRepository<AchievementOperationLog, Long>, JpaSpecificationExecutor<AchievementOperationLog> {

    /**
     * 根据项目ID查询
     */
    Page<AchievementOperationLog> findByProjectIdOrderByOperationTimeDesc(Long projectId, Pageable pageable);

    /**
     * 根据项目ID和时间范围查询
     */
    Page<AchievementOperationLog> findByProjectIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据项目ID和成果ID查询
     */
    Page<AchievementOperationLog> findByProjectIdAndAchievementIdOrderByOperationTimeDesc(
            Long projectId, Long achievementId, Pageable pageable);

    /**
     * 根据项目ID和用户ID查询
     */
    Page<AchievementOperationLog> findByProjectIdAndUserIdOrderByOperationTimeDesc(
            Long projectId, Long userId, Pageable pageable);

    /**
     * 根据项目ID和操作类型查询
     */
    Page<AchievementOperationLog> findByProjectIdAndOperationTypeOrderByOperationTimeDesc(
            Long projectId, AchievementOperationType operationType, Pageable pageable);

    /**
     * 组合查询：项目+成果+时间范围
     */
    Page<AchievementOperationLog> findByProjectIdAndAchievementIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, Long achievementId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 组合查询：项目+用户+时间范围
     */
    Page<AchievementOperationLog> findByProjectIdAndUserIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 动态组合查询
     */
    @Query("SELECT a FROM AchievementOperationLog a WHERE a.projectId = :projectId " +
            "AND (:achievementId IS NULL OR a.achievementId = :achievementId) " +
            "AND (:userId IS NULL OR a.userId = :userId) " +
            "AND (:operationType IS NULL OR a.operationType = :operationType) " +
            "AND (:startTime IS NULL OR a.operationTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.operationTime <= :endTime) " +
            "ORDER BY a.operationTime DESC")
    Page<AchievementOperationLog> findByConditions(
            @Param("projectId") Long projectId,
            @Param("achievementId") Long achievementId,
            @Param("userId") Long userId,
            @Param("operationType") AchievementOperationType operationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 统计项目的成果操作总数
     */
    long countByProjectId(Long projectId);

    /**
     * 统计用户在项目中的成果操作数
     */
    long countByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * 按日期统计项目成果贡献数
     * 返回日期和贡献数的映射
     *
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日期字符串（yyyy-MM-dd）和贡献数的映射
     */
    @Query(value = "SELECT DATE(operation_time) as date, COUNT(*) as count " +
            "FROM zhiyanactivelog.achievement_operation_log " +
            "WHERE project_id = :projectId " +
            "AND operation_time >= :startTime " +
            "AND operation_time <= :endTime " +
            "GROUP BY DATE(operation_time) " +
            "ORDER BY date ASC",
            nativeQuery = true)
    List<Object[]> countContributionsByDate(
            @Param("projectId") Long projectId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
