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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 任务用户关联Repository
 *
 * @author Tokito
 */
@Repository
public interface TaskUserRepository extends JpaRepository<TaskUser, Long> {

    @Query("SELECT tu FROM TaskUser tu WHERE tu.taskId = :taskId AND tu.isActive = true ORDER BY tu.assignedAt ASC")
    List<TaskUser> findActiveExecutorsByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.isActive = true ORDER BY tu.assignedAt DESC")
    Page<TaskUser> findActiveTasksByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.projectId = :projectId AND tu.isActive = true ORDER BY tu.assignedAt DESC")
    List<TaskUser> findActiveTasksByUserAndProject(@Param("userId") Long userId,
                                                   @Param("projectId") Long projectId);

    @Query("SELECT CASE WHEN COUNT(tu) > 0 THEN true ELSE false END FROM TaskUser tu WHERE tu.taskId = :taskId AND tu.userId = :userId AND tu.isActive = true")
    boolean isUserActiveExecutor(@Param("taskId") Long taskId, @Param("userId") Long userId);

    Optional<TaskUser> findByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT COUNT(tu) FROM TaskUser tu WHERE tu.userId = :userId AND tu.isActive = true")
    long countActiveTasksByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(tu) FROM TaskUser tu WHERE tu.taskId = :taskId AND tu.isActive = true")
    long countActiveExecutorsByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isActive = false, tu.removedAt = :removedAt, tu.removedBy = :removedBy, tu.updatedAt = :removedAt WHERE tu.taskId = :taskId AND tu.isActive = true")
    int deactivateTaskAssignees(@Param("taskId") Long taskId,
                                @Param("removedAt") Instant removedAt,
                                @Param("removedBy") Long removedBy);

    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isActive = false, tu.removedAt = :removedAt, tu.removedBy = :removedBy, tu.updatedAt = :removedAt WHERE tu.taskId = :taskId AND tu.userId = :userId AND tu.isActive = true")
    int deactivateTaskUser(@Param("taskId") Long taskId,
                           @Param("userId") Long userId,
                           @Param("removedAt") Instant removedAt,
                           @Param("removedBy") Long removedBy);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.isActive = :isActive")
    List<TaskUser> findByUserIdAndIsActive(@Param("userId") Long userId,
                                           @Param("isActive") Boolean isActive);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.userId = :userId AND tu.assignType = :assignType AND tu.isActive = :isActive")
    Page<TaskUser> findByUserIdAndAssignTypeAndIsActive(@Param("userId") Long userId,
                                                        @Param("assignType") AssignType assignType,
                                                        @Param("isActive") Boolean isActive,
                                                        Pageable pageable);
}
