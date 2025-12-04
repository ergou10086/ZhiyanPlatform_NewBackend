package hbnu.project.zhiyanbackend.activelog.repository;

import hbnu.project.zhiyanbackend.activelog.model.entity.TaskOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.enums.TaskOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 任务操作日志Repository
 *
 * @author ErgouTree
 */
@Repository
public interface TaskOperationLogRepository extends JpaRepository<TaskOperationLog,Long>, JpaSpecificationExecutor<TaskOperationLog> {

    /**
     * 根据项目ID查询
     */
    Page<TaskOperationLog> findByProjectIdOrderByOperationTimeDesc(Long projectId, Pageable pageable);

    /**
     * 根据项目ID和时间范围查询
     */
    Page<TaskOperationLog> findByProjectIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据项目ID和任务ID查询
     */
    Page<TaskOperationLog> findByProjectIdAndTaskIdOrderByOperationTimeDesc(Long projectId, Long taskId, Pageable pageable);

    /**
     * 根据项目ID和用户ID查询
     */
    Page<TaskOperationLog> findByProjectIdAndUserIdOrderByOperationTimeDesc(
            Long projectId, Long userId, Pageable pageable);

    /**
     * 根据项目ID和操作类型查询
     */
    Page<TaskOperationLog> findByProjectIdAndOperationTypeOrderByOperationTimeDesc(Long projectId, TaskOperationType operationType, Pageable pageable);

    /**
     * 组合查询：项目+任务+时间范围
     */
    Page<TaskOperationLog> findByProjectIdAndTaskIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, Long taskId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 组合查询：项目+用户+时间范围（
     */
    Page<TaskOperationLog> findByProjectIdAndUserIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long projectId, Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 动态组合查询
     */
    @Query("SELECT t FROM TaskOperationLog t WHERE t.projectId = :projectId " +
            "AND (:taskId IS NULL OR t.taskId = :taskId) " +
            "AND (:userId IS NULL OR t.userId = :userId) " +
            "AND (:operationType IS NULL OR t.operationType = :operationType) " +
            "AND (:startTime IS NULL OR t.operationTime >= :startTime) " +
            "AND (:endTime IS NULL OR t.operationTime <= :endTime) " +
            "ORDER BY t.operationTime DESC")
    Page<TaskOperationLog> findByConditions(
            @Param("projectId") Long projectId,
            @Param("taskId") Long taskId,
            @Param("userId") Long userId,
            @Param("operationType") TaskOperationType operationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 统计项目的任务操作总数
     */
    long countByProjectId(Long projectId);

    /**
     * 统计用户在项目中的任务操作数
     */
    long countByProjectIdAndUserId(Long projectId, Long userId);
}
