package hbnu.project.zhiyanbackend.activelog.repository;

import hbnu.project.zhiyanbackend.activelog.model.entity.WikiOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.enums.WikiOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Wiki操作日志Repository
 *
 * @author ErgouTree
 */
@Repository
public interface WikiOperationLogRepository extends JpaRepository<WikiOperationLog,Long>, JpaSpecificationExecutor<WikiOperationLog> {

    /**
     * 根据项目ID查询
     */
    Page<WikiOperationLog> findByProjectIdOrderByOperationTimeDesc(Long projectId, Pageable pageable);

    /**
     * 根据项目ID和时间范围查询
     */
    Page<WikiOperationLog> findByProjectIdAndOperationTimeBetweenOrderByOperationTimeDesc(Long projectId, LocalDateTime startTime,  LocalDateTime endTime, Pageable pageable);

    /**
     * 根据项目ID和Wiki页面ID查询
     */
    Page<WikiOperationLog> findByProjectIdAndWikiPageIdOrderByOperationTimeDesc(Long projectId, Long wikiPageId, Pageable pageable);

    /**
     * 根据项目ID和用户ID查询
     */
    Page<WikiOperationLog> findByProjectIdAndUserIdOrderByOperationTimeDesc(Long projectId, Long userId, Pageable pageable);

    /**
     * 根据项目ID和操作类型查询
     */
    Page<WikiOperationLog> findByProjectIdAndOperationTypeOrderByOperationTimeDesc(Long projectId, WikiOperationType operationType, Pageable pageable);

    /**
     * 组合查询：项目+Wiki页面+时间范围
     */
    Page<WikiOperationLog> findByProjectIdAndWikiPageIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, Long wikiPageId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 组合查询：项目+用户+时间范围
     */
    Page<WikiOperationLog> findByProjectIdAndUserIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 动态组合查询（适用于复杂筛选场景）
     */
    @Query("SELECT w FROM WikiOperationLog w WHERE w.projectId = :projectId " +
            "AND (:wikiPageId IS NULL OR w.wikiPageId = :wikiPageId) " +
            "AND (:userId IS NULL OR w.userId = :userId) " +
            "AND (:operationType IS NULL OR w.operationType = :operationType) " +
            "AND (:startTime IS NULL OR w.operationTime >= :startTime) " +
            "AND (:endTime IS NULL OR w.operationTime <= :endTime) " +
            "ORDER BY w.operationTime DESC")
    Page<WikiOperationLog> findByConditions(
            @Param("projectId") Long projectId,
            @Param("wikiPageId") Long wikiPageId,
            @Param("userId") Long userId,
            @Param("operationType") WikiOperationType operationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 统计项目的Wiki操作总数
     */
    long countByProjectId(Long projectId);

    /**
     * 统计用户在项目中的Wiki操作数
     */
    long countByProjectIdAndUserId(Long projectId, Long userId);
}
