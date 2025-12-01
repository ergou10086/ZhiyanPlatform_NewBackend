package hbnu.project.zhiyanbackend.tasks.repository;

import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskUser;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskPriority;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 任务数据访问层
 *
 * @author Tokito
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // 按项目和删除标记查询全部任务列表
    List<Task> findByProjectIdAndIsDeleted(Long projectId, Boolean isDeleted);

    Page<Task> findByProjectIdAndIsDeleted(Long projectId, Boolean isDeleted, Pageable pageable);

    Page<Task> findByProjectIdAndStatusAndIsDeleted(Long projectId, TaskStatus status, Boolean isDeleted, Pageable pageable);

    Page<Task> findByProjectIdAndPriorityAndIsDeleted(Long projectId, TaskPriority priority, Boolean isDeleted, Pageable pageable);

    Page<Task> findByCreatorIdAndIsDeleted(Long creatorId, Boolean isDeleted, Pageable pageable);

    Page<Task> findByCreatorIdAndStatusAndIsDeleted(Long creatorId, TaskStatus status, Boolean isDeleted, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.isDeleted = false AND (t.title LIKE %:keyword% OR t.description LIKE %:keyword%)")
    Page<Task> searchByKeyword(@Param("projectId") Long projectId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.isDeleted = false " +
            "AND t.dueDate >= :today AND t.dueDate <= :targetDate " +
            "AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE")
    Page<Task> findUpcomingTasks(@Param("projectId") Long projectId,
                                 @Param("today") LocalDate today,
                                 @Param("targetDate") LocalDate targetDate,
                                 Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.isDeleted = false " +
            "AND t.dueDate < :today AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE")
    Page<Task> findOverdueTasks(@Param("projectId") Long projectId,
                                @Param("today") LocalDate today,
                                Pageable pageable);

    @Query("SELECT t FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
            "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false " +
            "AND t.dueDate >= :today AND t.dueDate <= :targetDate " +
            "AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE " +
            "ORDER BY t.dueDate ASC")
    Page<Task> findMyUpcomingTasks(@Param("userId") Long userId,
                                   @Param("today") LocalDate today,
                                   @Param("targetDate") LocalDate targetDate,
                                   Pageable pageable);

    @Query("SELECT t FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
            "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false " +
            "AND t.dueDate < :today AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE " +
            "ORDER BY t.dueDate DESC")
    Page<Task> findMyOverdueTasks(@Param("userId") Long userId,
                                  @Param("today") LocalDate today,
                                  Pageable pageable);
    @Query("""
    SELECT t FROM Task t
    WHERE t.id IN (:taskIds)
      AND t.projectId = :projectId
      AND (t.isDeleted = false OR t.isDeleted IS NULL)
    """)
    List<Task> findActiveByIdsAndProject(@Param("taskIds") Collection<Long> taskIds,
                                         @Param("projectId") Long projectId);

    @Query("""
    SELECT DISTINCT t FROM Task t
    INNER JOIN AchievementTaskRef ref ON t.id = ref.taskId
    WHERE ref.achievementId = :achievementId
      AND (t.isDeleted = false OR t.isDeleted IS NULL)
    ORDER BY ref.createdAt DESC
    """)
    List<Task> findByAchievementIdWithJoin(@Param("achievementId") Long achievementId);

    @Query("""
    SELECT DISTINCT t FROM Task t
    LEFT JOIN FETCH TaskUser tu ON t.id = tu.taskId AND tu.isActive = true
    WHERE t.id IN (:taskIds)
      AND (t.isDeleted = false OR t.isDeleted IS NULL)
    """)
    List<Task> findByIdsWithExecutors(@Param("taskIds") Collection<Long> taskIds);
}
