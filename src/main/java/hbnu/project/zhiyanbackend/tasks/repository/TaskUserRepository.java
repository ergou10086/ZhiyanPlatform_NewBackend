package hbnu.project.zhiyanbackend.tasks.repository;

import hbnu.project.zhiyanbackend.tasks.model.entity.TaskUser;
import hbnu.project.zhiyanbackend.tasks.model.enums.AssignType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 任务与用户关联关系（TaskUser）的数据访问层
 *
 * @author Tokito
 */
@Repository
public interface TaskUserRepository extends JpaRepository<TaskUser, Long> {

    /**
     * 查询指定任务下所有活跃的执行人（即 isActive = true 的 TaskUser），按分配时间升序排列。
     *
     * @param taskId 任务ID
     * @return 活跃执行人列表（按 assignedAt 升序）
     */
    @Query("SELECT tu FROM TaskUser tu WHERE tu.taskId = :taskId AND tu.isActive = true ORDER BY tu.assignedAt ASC")
    List<TaskUser> findActiveExecutorsByTaskId(@Param("taskId") Long taskId);

    /**
     * 批量查询指定任务列表下所有活跃的执行人（即 isActive = true 的 TaskUser），按任务ID和分配时间升序排列。
     *
     * @param taskIds 任务ID列表
     * @param isActive 是否活跃
     * @return 活跃执行人列表（按 taskId 和 assignedAt 升序）
     */
    @Query("SELECT tu FROM TaskUser tu WHERE tu.taskId IN :taskIds AND tu.isActive = :isActive ORDER BY tu.taskId ASC, tu.assignedAt ASC")
    List<TaskUser> findByTaskIdInAndIsActive(@Param("taskIds") List<Long> taskIds, @Param("isActive") Boolean isActive);

    /**
     * 分页查询指定用户当前参与的所有活跃任务分配记录（isActive = true），按分配时间降序。
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 用户参与的活跃任务分配分页列表
     */
    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.isActive = true ORDER BY tu.assignedAt DESC")
    Page<TaskUser> findActiveTasksByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 查询指定用户在指定项目中所有活跃的任务分配记录（isActive = true），按分配时间降序。
     *
     * @param userId     用户ID
     * @param projectId  项目ID
     * @return 该用户在该项目中的活跃任务分配列表
     */
    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.projectId = :projectId AND tu.isActive = true ORDER BY tu.assignedAt DESC")
    List<TaskUser> findActiveTasksByUserAndProject(@Param("userId") Long userId,
                                                   @Param("projectId") Long projectId);

    /**
     * 判断指定用户是否是某任务的活跃执行人。
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 如果存在 isActive = true 的关联记录，则返回 true；否则 false
     */
    @Query("SELECT CASE WHEN COUNT(tu) > 0 THEN true ELSE false END FROM TaskUser tu WHERE tu.taskId = :taskId AND tu.userId = :userId AND tu.isActive = true")
    boolean isUserActiveExecutor(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /**
     * 根据任务ID和用户ID精确查找一条 TaskUser 记录（无论是否活跃）。
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return Optional 包装的 TaskUser 实体，若不存在则为空
     */
    Optional<TaskUser> findByTaskIdAndUserId(Long taskId, Long userId);

    /**
     * 统计指定用户当前参与的活跃任务总数（isActive = true）。
     *
     * @param userId 用户ID
     * @return 活跃任务数量
     */
    @Query("SELECT COUNT(tu) FROM TaskUser tu WHERE tu.userId = :userId AND tu.isActive = true")
    long countActiveTasksByUserId(@Param("userId") Long userId);

    /**
     * 统计指定任务当前拥有的活跃执行人数量（isActive = true）。
     *
     * @param taskId 任务ID
     * @return 活跃执行人数量
     */
    @Query("SELECT COUNT(tu) FROM TaskUser tu WHERE tu.taskId = :taskId AND tu.isActive = true")
    long countActiveExecutorsByTaskId(@Param("taskId") Long taskId);

    /**
     * 批量取消（软删除）某任务下的所有活跃分配记录。
     * <p>
     * 将 isActive 设为 false，并记录移除时间和操作人。
     * </p>
     *
     * @param taskId     任务ID
     * @param removedAt  移除时间
     * @param removedBy  操作人用户ID
     * @return 被更新的记录数
     */
    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isActive = false, tu.removedAt = :removedAt, tu.removedBy = :removedBy, tu.updatedAt = :removedAt WHERE tu.taskId = :taskId AND tu.isActive = true")
    int deactivateTaskAssignees(@Param("taskId") Long taskId,
                                @Param("removedAt") LocalDateTime removedAt,
                                @Param("removedBy") Long removedBy);

    /**
     * 取消（软删除）某任务对特定用户的分配。
     * <p>
     * 仅当该分配当前处于活跃状态时生效。
     * </p>
     *
     * @param taskId     任务ID
     * @param userId     用户ID
     * @param removedAt  移除时间
     * @param removedBy  操作人用户ID
     * @return 被更新的记录数（通常为 0 或 1）
     */
    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isActive = false, tu.removedAt = :removedAt, tu.removedBy = :removedBy, tu.updatedAt = :removedAt WHERE tu.taskId = :taskId AND tu.userId = :userId AND tu.isActive = true")
    int deactivateTaskUser(@Param("taskId") Long taskId,
                           @Param("userId") Long userId,
                           @Param("removedAt") LocalDateTime removedAt,
                           @Param("removedBy") Long removedBy);

    /**
     * 查询指定用户的所有任务分配记录（按活跃状态筛选）。
     *
     * @param userId    用户ID
     * @param isActive  是否活跃（true=活跃，false=已移除）
     * @return 符合条件的 TaskUser 列表
     */
    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.isActive = :isActive")
    List<TaskUser> findByUserIdAndIsActive(@Param("userId") Long userId,
                                           @Param("isActive") Boolean isActive);

    /**
     * 分页查询指定用户、特定分配类型（如 MANUAL、AUTO 等）且指定活跃状态的任务分配记录。
     *
     * @param userId      用户ID
     * @param assignType  分配类型（枚举）
     * @param isActive    是否活跃
     * @param pageable    分页参数
     * @return 分页后的任务分配列表
     */
    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.assignType = :assignType AND tu.isActive = :isActive")
    Page<TaskUser> findByUserIdAndAssignTypeAndIsActive(@Param("userId") Long userId,
                                                        @Param("assignType") AssignType assignType,
                                                        @Param("isActive") Boolean isActive,
                                                        Pageable pageable);
}