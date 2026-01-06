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
import java.util.Optional;

/**
 * 任务（Task）实体的数据访问层（Repository）
 *
 * @author Tokito
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 根据项目ID和软删除标记查询所有任务列表（不分页）。
     *
     * @param projectId   项目ID
     * @param isDeleted   是否已删除（true=已删除，false=未删除）
     * @return 符合条件的任务列表
     */
    List<Task> findByProjectIdAndIsDeleted(Long projectId, Boolean isDeleted);

    /**
     * 分页查询指定项目中未被软删除（或根据标记）的任务。
     *
     * @param projectId   项目ID
     * @param isDeleted   软删除标记
     * @param pageable    分页参数
     * @return 分页后的任务列表
     */
    Page<Task> findByProjectIdAndIsDeleted(Long projectId, Boolean isDeleted, Pageable pageable);

    /**
     * 分页查询指定项目中特定状态且未被软删除的任务。
     *
     * @param projectId   项目ID
     * @param status      任务状态（如 TODO、IN_PROGRESS、DONE 等）
     * @param isDeleted   软删除标记
     * @param pageable    分页参数
     * @return 分页后的任务列表
     */
    Page<Task> findByProjectIdAndStatusAndIsDeleted(Long projectId, TaskStatus status, Boolean isDeleted, Pageable pageable);

    /**
     * 分页查询指定项目中特定优先级且未被软删除的任务。
     *
     * @param projectId   项目ID
     * @param priority    任务优先级（如 LOW、MEDIUM、HIGH）
     * @param isDeleted   软删除标记
     * @param pageable    分页参数
     * @return 分页后的任务列表
     */
    Page<Task> findByProjectIdAndPriorityAndIsDeleted(Long projectId, TaskPriority priority, Boolean isDeleted, Pageable pageable);

    /**
     * 分页查询由指定用户创建且未被软删除的任务。
     *
     * @param creatorId   创建者用户ID
     * @param isDeleted   软删除标记
     * @param pageable    分页参数
     * @return 分页后的任务列表
     */
    Page<Task> findByCreatorIdAndIsDeleted(Long creatorId, Boolean isDeleted, Pageable pageable);

    /**
     * 分页查询由指定用户创建、处于特定状态且未被软删除的任务。
     *
     * @param creatorId   创建者用户ID
     * @param status      任务状态
     * @param isDeleted   软删除标记
     * @param pageable    分页参数
     * @return 分页后的任务列表
     */
    Page<Task> findByCreatorIdAndStatusAndIsDeleted(Long creatorId, TaskStatus status, Boolean isDeleted, Pageable pageable);

    /**
     * 在指定项目中根据关键词模糊搜索任务标题或描述（仅限未删除任务）。
     *
     * @param projectId   项目ID
     * @param keyword     搜索关键词
     * @param pageable    分页参数
     * @return 匹配关键词的分页任务列表
     */
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.isDeleted = false AND (t.title LIKE %:keyword% OR t.description LIKE %:keyword%)")
    Page<Task> searchByKeyword(@Param("projectId") Long projectId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 分页查询当前用户参与的任务（基于 TaskUser 分配记录，按分配时间倒序）
     * 用于 /tasks/my-assigned
     */
    @Query(
            value = "SELECT t FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
                    "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false " +
                    "ORDER BY tu.assignedAt DESC",
            countQuery = "SELECT COUNT(t) FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
                    "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false"
    )
    Page<Task> findMyAssignedTasks(@Param("userId") Long userId, Pageable pageable);

    /**
     * 分页查询当前用户参与的任务（基于 TaskUser 分配记录，按分配时间倒序），并按截止日期过滤
     * 用于 /tasks/my-assigned
     */
    @Query(
            value = "SELECT t FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
                    "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false " +
                    "AND t.dueDate IS NOT NULL AND t.dueDate >= :startDate AND t.dueDate <= :endDate " +
                    "ORDER BY t.dueDate ASC",
            countQuery = "SELECT COUNT(t) FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
                    "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false " +
                    "AND t.dueDate IS NOT NULL AND t.dueDate >= :startDate AND t.dueDate <= :endDate"
    )
    Page<Task> findMyAssignedTasksByDueDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * 查询指定项目中在指定日期范围内即将到期（但未完成）的任务。
     *
     * @param projectId    项目ID
     * @param today        当前日期（用于过滤未来任务）
     * @param targetDate   目标截止日期（如未来7天）
     * @param pageable     分页参数
     * @return 即将到期的分页任务列表（按截止日期升序）
     */
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.isDeleted = false " +
            "AND t.dueDate >= :today AND t.dueDate <= :targetDate " +
            "AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE")
    Page<Task> findUpcomingTasks(@Param("projectId") Long projectId,
                                 @Param("today") LocalDate today,
                                 @Param("targetDate") LocalDate targetDate,
                                 Pageable pageable);

    /**
     * 查询指定项目中已逾期（截止日期早于今天）且未完成的任务。
     *
     * @param projectId   项目ID
     * @param today       当前日期
     * @param pageable    分页参数
     * @return 逾期任务的分页列表（按截止日期降序）
     */
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.isDeleted = false " +
            "AND t.dueDate < :today AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE")
    Page<Task> findOverdueTasks(@Param("projectId") Long projectId,
                                @Param("today") LocalDate today,
                                Pageable pageable);

    /**
     * 查询分配给指定用户、在指定日期范围内即将到期且未完成的任务。
     *
     * @param userId       用户ID
     * @param today        当前日期
     * @param targetDate   目标截止日期
     * @param pageable     分页参数
     * @return 用户即将到期任务的分页列表（按截止日期升序）
     */
    @Query("SELECT t FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
            "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false " +
            "AND t.dueDate >= :today AND t.dueDate <= :targetDate " +
            "AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE " +
            "ORDER BY t.dueDate ASC")
    Page<Task> findMyUpcomingTasks(@Param("userId") Long userId,
                                   @Param("today") LocalDate today,
                                   @Param("targetDate") LocalDate targetDate,
                                   Pageable pageable);

    /**
     * 查询分配给指定用户、已逾期且未完成的任务。
     *
     * @param userId     用户ID
     * @param today      当前日期
     * @param pageable   分页参数
     * @return 用户逾期任务的分页列表（按截止日期降序）
     */
    @Query("SELECT t FROM Task t JOIN TaskUser tu ON t.id = tu.taskId " +
            "WHERE tu.userId = :userId AND tu.isActive = true AND t.isDeleted = false " +
            "AND t.dueDate < :today AND t.status <> hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.DONE " +
            "ORDER BY t.dueDate DESC")
    Page<Task> findMyOverdueTasks(@Param("userId") Long userId,
                                  @Param("today") LocalDate today,
                                  Pageable pageable);

    /**
     * 根据任务ID集合和项目ID，查询有效的（未删除）任务列表。
     *
     * @param taskIds    任务ID集合
     * @param projectId  项目ID（用于权限校验）
     * @return 有效任务列表
     */
    @Query("""
    SELECT t FROM Task t
    WHERE t.id IN (:taskIds)
      AND t.projectId = :projectId
      AND (t.isDeleted = false OR t.isDeleted IS NULL)
    """)
    List<Task> findActiveByIdsAndProject(@Param("taskIds") Collection<Long> taskIds,
                                         @Param("projectId") Long projectId);

    /**
     * 根据成果ID查询关联的所有任务（通过 AchievementTaskRef 关联），并按关联时间倒序排列。
     *
     * @param achievementId 成果ID
     * @return 关联的任务列表（按创建时间倒序）
     */
    @Query("""
    SELECT DISTINCT t FROM Task t
    INNER JOIN AchievementTaskRef ref ON t.id = ref.taskId
    WHERE ref.achievementId = :achievementId
      AND (t.isDeleted = false OR t.isDeleted IS NULL)
    ORDER BY ref.createdAt DESC
    """)
    List<Task> findByAchievementIdWithJoin(@Param("achievementId") Long achievementId);

    /**
     * 根据任务ID集合查询任务，并预加载活跃的执行人（TaskUser）信息。
     *
     * @param taskIds 任务ID集合
     * @return 带执行人信息的任务列表
     */
    @Query("""
    SELECT DISTINCT t FROM Task t
    LEFT JOIN FETCH TaskUser tu ON t.id = tu.taskId AND tu.isActive = true
    WHERE t.id IN (:taskIds)
      AND (t.isDeleted = false OR t.isDeleted IS NULL)
    """)
    List<Task> findByIdsWithExecutors(@Param("taskIds") Collection<Long> taskIds);

    /**
     * 查询所有需要发送提醒的任务：状态为 TODO 或 IN_PROGRESS、有截止日期、且截止日期在未来。
     *
     * @param currentDate 当前日期
     * @param pageable    分页参数
     * @return 需要提醒的任务分页列表
     */
    @Query("""
        SELECT t FROM Task t
        WHERE t.isDeleted = false 
        AND t.status IN (hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.TODO, hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.IN_PROGRESS)
        AND t.dueDate IS NOT NULL 
        AND t.dueDate > :currentDate
        """)
    Page<Task> findTasksForReminder(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    /**
     * 查询已分配给指定用户但该用户尚未提交任何有效提交记录的任务。
     * <p>
     * 条件包括：
     * - 用户被分配到任务（TaskUser.isActive = true）
     * - 任务未被软删除
     * - 任务状态为 TODO 或 IN_PROGRESS
     * - 不存在该用户的非删除状态的 TaskSubmission
     * </p>
     *
     * @param userId    用户ID
     * @param pageable  分页参数
     * @return 未提交任务的分页列表
     */
    @Query("""
        SELECT t FROM Task t
        INNER JOIN TaskUser tu ON t.id = tu.taskId
        WHERE tu.userId = :userId
            AND tu.isActive = true
            AND t.isDeleted = false
            AND t.status IN (hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.TODO,
                             hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus.IN_PROGRESS)
            AND NOT EXISTS (
                      SELECT 1 FROM TaskSubmission s
                      WHERE s.taskId = t.id
                        AND s.submitterId = :userId
                        AND s.isDeleted = false
                  )
    """)
    Page<Task> findUnsubmittedTasksByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 统计指定用户“已分配但未提交”的任务数量。
     * <p>
     * 逻辑同 {@link #findUnsubmittedTasksByUserId}，但仅返回总数。
     * </p>
     *
     * @param userId 用户ID
     * @return 未提交任务的数量
     */
    @Query("""
    SELECT COUNT(t) FROM Task t
    INNER JOIN TaskUser tu ON t.id = tu.taskId
    WHERE tu.userId = :userId
      AND tu.isActive = true
      AND t.isDeleted = false
      AND NOT EXISTS (
          SELECT 1 FROM TaskSubmission s 
          WHERE s.taskId = t.id 
            AND s.submitterId = :userId 
            AND s.isDeleted = false
      )
    """)
    long countUnsubmittedTasksByUserId(@Param("userId") Long userId);

    /**
     * 统计指定项目中未被软删除的任务总数。
     *
     * @param projectId 项目ID
     * @return 有效任务数量
     */
    long countByProjectIdAndIsDeletedFalse(Long projectId);

    /**
     * 批量统计多个项目中未被软删除的任务数量，按项目ID分组返回。
     *
     * @param projectIds 项目ID集合
     * @return 每个项目对应的任务数量（Object[0]=projectId, Object[1]=count）
     */
    @Query("""
        SELECT t.projectId, COUNT(t) FROM Task t
        WHERE t.projectId IN (:projectIds)
          AND t.isDeleted = false
        GROUP BY t.projectId
        """)
    List<Object[]> countTasksByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    /**
     * 批量查询任务的基本信息（id, title, creatorId, dueDate），用于任务提交列表等场景
     * 避免查询完整的Task实体，提高性能
     */
    @Query("SELECT t.id, t.title, t.creatorId, t.dueDate FROM Task t WHERE t.id IN :taskIds AND t.isDeleted = false")
    List<Object[]> findBasicInfoByIdInAndIsDeletedFalse(@Param("taskIds") List<Long> taskIds);

    /**
     * 查询任务的基本信息（title, creatorId, dueDate），用于单个任务查询
     * 避免查询完整的Task实体，提高性能
     */
    @Query("SELECT t.title, t.creatorId, t.dueDate FROM Task t WHERE t.id = :taskId AND t.isDeleted = false")
    Optional<Object[]> findBasicInfoById(@Param("taskId") Long taskId);
}