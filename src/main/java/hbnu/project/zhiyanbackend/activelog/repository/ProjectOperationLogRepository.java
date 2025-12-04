package hbnu.project.zhiyanbackend.activelog.repository;

import hbnu.project.zhiyanbackend.activelog.model.entity.ProjectOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.enums.ProjectOperationType;
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
 * 项目操作日志Repository
 *
 * @author ErgouTree
 */
@Repository
public interface ProjectOperationLogRepository extends JpaRepository<ProjectOperationLog,Long>, JpaSpecificationExecutor<ProjectOperationLog> {

    /**
     * 根据项目ID查询
     * 使用索引：idx_project_time
     */
    Page<ProjectOperationLog> findByProjectIdOrderByOperationTimeDesc(Long projectId, Pageable pageable);

    /**
     * 根据项目ID和时间范围查询
     * 使用索引：idx_project_time
     */
    Page<ProjectOperationLog> findByProjectIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据项目ID和用户ID查询
     * 使用索引：idx_project_user_time
     */
    Page<ProjectOperationLog> findByProjectIdAndUserIdOrderByOperationTimeDesc(
            Long projectId, Long userId, Pageable pageable);

    /**
     * 根据项目ID和操作类型查询
     */
    Page<ProjectOperationLog> findByProjectIdAndOperationTypeOrderByOperationTimeDesc(
            Long projectId, ProjectOperationType operationType, Pageable pageable);

    /**
     * 组合查询：项目+用户+时间范围
     * 使用索引：idx_project_user_time
     */
    Page<ProjectOperationLog> findByProjectIdAndUserIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 动态组合查询
     */
    @Query("SELECT p FROM ProjectOperationLog p WHERE p.projectId = :projectId " +
            "AND (:userId IS NULL OR p.userId = :userId) " +
            "AND (:operationType IS NULL OR p.operationType = :operationType) " +
            "AND (:startTime IS NULL OR p.operationTime >= :startTime) " +
            "AND (:endTime IS NULL OR p.operationTime <= :endTime) " +
            "ORDER BY p.operationTime DESC")
    Page<ProjectOperationLog> findByConditions(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("operationType") ProjectOperationType operationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 统计项目的操作总数
     */
    long countByProjectId(Long projectId);

    /**
     * 统计用户在项目中的操作数
     */
    long countByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * 查询用户参与的所有项目（去重）
     */
    @Query("SELECT DISTINCT p.projectId FROM ProjectOperationLog p WHERE p.userId = :userId")
    List<Long> findDistinctProjectIdsByUserId(@Param("userId") Long userId);
}
