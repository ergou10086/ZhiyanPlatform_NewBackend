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
 */
@Repository
public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    List<TaskSubmission> findByTaskIdAndIsDeletedFalseOrderByVersionDesc(Long taskId);

    Optional<TaskSubmission> findFirstByTaskIdAndIsDeletedFalseOrderByVersionDesc(Long taskId);

    Optional<TaskSubmission> findByTaskIdAndIsFinalTrueAndIsDeletedFalse(Long taskId);

    Page<TaskSubmission> findByReviewStatusAndIsDeletedFalseOrderBySubmissionTimeDesc(
            ReviewStatus reviewStatus, Pageable pageable);

    Page<TaskSubmission> findByProjectIdAndReviewStatusAndIsDeletedFalseOrderBySubmissionTimeDesc(
            Long projectId, ReviewStatus reviewStatus, Pageable pageable);

    Page<TaskSubmission> findBySubmitterIdAndIsDeletedFalseOrderBySubmissionTimeDesc(
            Long submitterId, Pageable pageable);

    Page<TaskSubmission> findBySubmitterIdAndProjectIdAndIsDeletedFalseOrderBySubmissionTimeDesc(
            Long submitterId, Long projectId, Pageable pageable);

    long countByTaskIdAndIsDeletedFalse(Long taskId);

    long countByReviewStatusAndIsDeletedFalse(ReviewStatus reviewStatus);

    long countByProjectIdAndReviewStatusAndIsDeletedFalse(Long projectId, ReviewStatus reviewStatus);

    @Query("SELECT COALESCE(MAX(s.version), 0) + 1 FROM TaskSubmission s WHERE s.taskId = :taskId AND s.isDeleted = false")
    Integer getNextVersionNumber(@Param("taskId") Long taskId);

    @Query("SELECT ts FROM TaskSubmission ts WHERE ts.reviewStatus = :reviewStatus AND ts.isDeleted = false " +
            "AND ts.taskId IN (SELECT t.id FROM Task t WHERE t.creatorId = :creatorId AND t.isDeleted = false " +
            "AND t.projectId IN (SELECT p.id FROM Project p WHERE p.isDeleted = false)) " +
            "ORDER BY ts.submissionTime DESC")
    Page<TaskSubmission> findPendingSubmissionsForMyCreatedTasks(
            @Param("creatorId") Long creatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    @Query("SELECT s FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE t.creatorId = :taskCreatorId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false ORDER BY s.submissionTime DESC")
    Page<TaskSubmission> findPendingSubmissionsForReviewer(
            @Param("taskCreatorId") Long taskCreatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    @Query("SELECT COUNT(ts) FROM TaskSubmission ts WHERE ts.reviewStatus = :reviewStatus AND ts.isDeleted = false " +
            "AND ts.taskId IN (SELECT t.id FROM Task t WHERE t.creatorId = :creatorId AND t.isDeleted = false " +
            "AND t.projectId IN (SELECT p.id FROM Project p WHERE p.isDeleted = false))")
    long countPendingSubmissionsForMyCreatedTasks(
            @Param("creatorId") Long creatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    @Query("SELECT s FROM TaskSubmission s LEFT JOIN Task t ON s.taskId = t.id " +
            "WHERE s.reviewStatus = :reviewStatus AND s.isDeleted = false " +
            "AND (s.submitterId = :userId OR t.creatorId = :userId) " +
            "AND (t.isDeleted = false OR t.id IS NULL) ORDER BY s.submissionTime DESC")
    Page<TaskSubmission> findPendingSubmissionsForUser(
            @Param("userId") Long userId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM TaskSubmission s LEFT JOIN Task t ON s.taskId = t.id " +
            "WHERE s.reviewStatus = :reviewStatus AND s.isDeleted = false " +
            "AND (s.submitterId = :userId OR t.creatorId = :userId) " +
            "AND (t.isDeleted = false OR t.id IS NULL)")
    long countPendingSubmissionsForUser(
            @Param("userId") Long userId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    @Query("SELECT s FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE s.submitterId = :submitterId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false ORDER BY s.submissionTime DESC")
    Page<TaskSubmission> findMyPendingSubmissions(
            @Param("submitterId") Long submitterId,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE s.submitterId = :submitterId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false")
    long countMyPendingSubmissions(
            @Param("submitterId") Long submitterId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    @Query("SELECT COUNT(s) FROM TaskSubmission s JOIN Task t ON s.taskId = t.id " +
            "WHERE t.creatorId = :taskCreatorId AND s.reviewStatus = :reviewStatus " +
            "AND s.isDeleted = false AND t.isDeleted = false")
    long countPendingSubmissionsForReviewer(
            @Param("taskCreatorId") Long taskCreatorId,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    @Query("SELECT s FROM TaskSubmission s WHERE s.taskId = :taskId AND s.isFinal = true " +
            "AND s.isDeleted = false ORDER BY s.version DESC")
    List<TaskSubmission> findFinalSubmissionsByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT s FROM TaskSubmission s WHERE s.taskId = :taskId AND s.isFinal = true " +
            "AND s.reviewStatus = 'APPROVED' AND s.isDeleted = false")
    List<TaskSubmission> findApprovedFinalSubmissionsByTaskId(@Param("taskId") Long taskId);
}
