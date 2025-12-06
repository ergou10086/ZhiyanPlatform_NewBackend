package hbnu.project.zhiyanbackend.tasks.repository;

import hbnu.project.zhiyanbackend.tasks.model.entity.TaskSubmission;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 任务提交记录Repository
 * 提供任务提交记录的数据访问操作
 *
 * @author Tokito
 */
@Repository
public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    /**
     * 根据任务ID查询提交记录，按版本号降序排列
     * @param taskId 任务ID
     * @return 提交记录列表
     */
    List<TaskSubmission> findByTaskIdAndIsDeletedFalseOrderByVersionDesc(Long taskId);

    /**
     * 根据任务ID查询最新版本的提交记录
     * @param taskId 任务ID
     * @return 最新版本的提交记录
     */
    Optional<TaskSubmission> findFirstByTaskIdAndIsDeletedFalseOrderByVersionDesc(Long taskId);

    /**
     * 根据审核状态查询提交记录，按提交时间降序排列
     * @param reviewStatus 审核状态
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    Page<TaskSubmission> findByReviewStatusAndIsDeletedFalseOrderBySubmissionTimeDesc(
            ReviewStatus reviewStatus, Pageable pageable);

    /**
     * 根据项目ID和审核状态查询提交记录，按提交时间降序排列
     * @param projectId 项目ID
     * @param reviewStatus 审核状态
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    Page<TaskSubmission> findByProjectIdAndReviewStatusAndIsDeletedFalseOrderBySubmissionTimeDesc(
            Long projectId, ReviewStatus reviewStatus, Pageable pageable);

    /**
     * 根据提交者ID查询提交记录，按提交时间降序排列
     * @param submitterId 提交者ID
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    Page<TaskSubmission> findBySubmitterIdAndIsDeletedFalseOrderBySubmissionTimeDesc(
            Long submitterId, Pageable pageable);

    /**
     * 根据提交者ID和项目ID查询提交记录，按提交时间降序排列
     * @param submitterId 提交者ID
     * @param projectId 项目ID
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    Page<TaskSubmission> findBySubmitterIdAndProjectIdAndIsDeletedFalseOrderBySubmissionTimeDesc(
            Long submitterId, Long projectId, Pageable pageable);

    /**
     * 统计指定任务的提交记录数量
     * @param taskId 任务ID
     * @return 提交记录数量
     */
    long countByTaskIdAndIsDeletedFalse(Long taskId);

    /**
     * 统计指定审核状态的提交记录数量
     * @param reviewStatus 审核状态
     * @return 提交记录数量
     */
    long countByReviewStatusAndIsDeletedFalse(ReviewStatus reviewStatus);

    /**
     * 统计指定项目和审核状态的提交记录数量
     * @param projectId 项目ID
     * @param reviewStatus 审核状态
     * @return 提交记录数量
     */
    long countByProjectIdAndReviewStatusAndIsDeletedFalse(Long projectId, ReviewStatus reviewStatus);

    /**
     * 获取任务提交的下一个版本号
     * @param taskId 任务ID
     * @return 下一个版本号
     */
    @Query("SELECT COALESCE(MAX(s.version), 0) + 1 FROM TaskSubmission s WHERE s.taskId = :taskId AND s.isDeleted = false")
    Integer getNextVersionNumber(@Param("taskId") Long taskId);

    /**
     * 查询用户创建的任务的待审核提交记录
     * @param creatorId 创建者ID
     * @param reviewStatus 审核状态
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    @Query("SELECT ts FROM TaskSubmission ts WHERE ts.reviewStatus = :reviewStatus AND ts.isDeleted = false " +
            "AND ts.taskId IN (SELECT t.id FROM Task t WHERE t.creatorId = :creatorId AND t.isDeleted = false " +
            "AND t.projectId IN (SELECT p.id FROM Project p WHERE p.isDeleted = false)) " +
            "ORDER BY ts.submissionTime DESC")
    Page<TaskSubmission> findPendingSubmissionsForMyCreatedTasks(
            @Param("creatorId") Long creatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    /**
     * 查询审核人需要审核的提交记录
     * @param taskCreatorId 任务创建者ID
     * @param reviewStatus 审核状态
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    @Query("SELECT s FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE t.creatorId = :taskCreatorId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false " +
            "AND s.version = (SELECT MAX(s2.version) FROM TaskSubmission s2 WHERE s2.taskId = s.taskId AND s2.isDeleted = false) " +
            "ORDER BY s.submissionTime DESC")
    Page<TaskSubmission> findPendingSubmissionsForReviewer(
            @Param("taskCreatorId") Long taskCreatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    /**
     * 统计用户创建的任务的待审核提交记录数量
     * @param creatorId 创建者ID
     * @param reviewStatus 审核状态
     * @return 提交记录数量
     */
    @Query("SELECT COUNT(ts) FROM TaskSubmission ts WHERE ts.reviewStatus = :reviewStatus AND ts.isDeleted = false " +
            "AND ts.taskId IN (SELECT t.id FROM Task t WHERE t.creatorId = :creatorId AND t.isDeleted = false " +
            "AND t.projectId IN (SELECT p.id FROM Project p WHERE p.isDeleted = false))")
    long countPendingSubmissionsForMyCreatedTasks(
            @Param("creatorId") Long creatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    /**
     * 查询用户相关的待审核提交记录
     * @param userId 用户ID
     * @param reviewStatus 审核状态
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    @Query("SELECT s FROM TaskSubmission s LEFT JOIN Task t ON s.taskId = t.id " +
            "WHERE s.reviewStatus = :reviewStatus AND s.isDeleted = false " +
            "AND (s.submitterId = :userId OR t.creatorId = :userId) " +
            "AND (t.isDeleted = false OR t.id IS NULL) ORDER BY s.submissionTime DESC")
    Page<TaskSubmission> findPendingSubmissionsForUser(
            @Param("userId") Long userId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    /**
     * 统计用户相关的待审核提交记录数量
     * @param userId 用户ID
     * @param reviewStatus 审核状态
     * @return 提交记录数量
     */
    @Query("SELECT COUNT(s) FROM TaskSubmission s LEFT JOIN Task t ON s.taskId = t.id " +
            "WHERE s.reviewStatus = :reviewStatus AND s.isDeleted = false " +
            "AND (s.submitterId = :userId OR t.creatorId = :userId) " +
            "AND (t.isDeleted = false OR t.id IS NULL)")
    long countPendingSubmissionsForUser(
            @Param("userId") Long userId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    /**
     * 查询用户提交的待审核记录
     * @param submitterId 提交者ID
     * @param reviewStatus 审核状态
     * @param pageable 分页参数
     * @return 分页后的提交记录
     */
    @Query("SELECT s FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE s.submitterId = :submitterId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false ORDER BY s.submissionTime DESC")
    Page<TaskSubmission> findMyPendingSubmissions(
            @Param("submitterId") Long submitterId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    /**
     * 统计用户提交的待审核记录数量
     * @param submitterId 提交者ID
     * @param reviewStatus 审核状态
     * @return 提交记录数量
     */
    @Query("SELECT COUNT(s) FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE s.submitterId = :submitterId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false")
    long countMyPendingSubmissions(
            @Param("submitterId") Long submitterId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    /**
     * 统计审核人需要审核的提交记录数量
     * @param taskCreatorId 任务创建者ID
     * @param reviewStatus 审核状态
     * @return 提交记录数量
     */
    @Query("SELECT COUNT(s) FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE t.creatorId = :taskCreatorId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false")
    long countPendingSubmissionsForReviewer(
            @Param("taskCreatorId") Long taskCreatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    /**
     * 批量查询任务的提交记录（用于批量获取附件）
     * @param taskIds 任务ID列表
     * @return 提交记录列表
     */
    List<TaskSubmission> findByTaskIdInAndIsDeletedFalseOrderByTaskIdAscVersionDesc(List<Long> taskIds);

}
