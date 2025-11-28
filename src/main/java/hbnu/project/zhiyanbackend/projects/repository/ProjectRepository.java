package hbnu.project.zhiyanbackend.projects.repository;

import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 项目数据访问层（精简版，保留主要查询能力）
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByName(String name);

    Page<Project> findByCreatorIdAndIsDeleted(Long creatorId, Boolean isDeleted, Pageable pageable);

    List<Project> findByCreatorIdAndIsDeleted(Long creatorId, Boolean isDeleted);

    Page<Project> findByStatusAndIsDeleted(ProjectStatus status, Boolean isDeleted, Pageable pageable);

    Page<Project> findByVisibilityAndIsDeleted(ProjectVisibility visibility, Boolean isDeleted, Pageable pageable);

    Page<Project> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Project> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByNameAndIdNot(String name, Long excludeId);

    boolean existsByNameAndIdNotAndIsDeleted(String name, Long excludeId, Boolean isDeleted);

    long countByCreatorId(Long creatorId);

    long countByStatus(ProjectStatus status);

    /**
     * 查询用户参与的项目（包括创建的和加入的，不包含已删除的项目）
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 项目分页列表
     */
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN ProjectMember pm ON p.id = pm.projectId " +
           "WHERE (p.creatorId = :userId OR pm.userId = :userId) AND p.isDeleted = false")
    Page<Project> findUserProjects(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.createdAt BETWEEN :startTime AND :endTime")
    Page<Project> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.visibility = 'PUBLIC' AND p.status != 'ARCHIVED' AND p.isDeleted = false")
    Page<Project> findPublicActiveProjects(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.isDeleted = false")
    Page<Project> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.isDeleted = false AND (p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    Page<Project> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p.name FROM Project p WHERE p.isDeleted = false AND p.id = :id")
    Optional<String> findProjectNameById(@Param("id") Long id);
}

