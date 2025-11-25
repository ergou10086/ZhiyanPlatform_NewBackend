package hbnu.project.zhiyanbackend.projects.repository;

import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目成员数据访问层（精简版，保留主要查询能力）
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    Page<ProjectMember> findByProjectId(Long projectId, Pageable pageable);

    List<ProjectMember> findByProjectId(Long projectId);

    List<ProjectMember> findByUserId(Long userId);

    Page<ProjectMember> findByUserId(Long userId, Pageable pageable);

    List<ProjectMember> findByProjectIdAndProjectRole(Long projectId, ProjectMemberRole projectRole);

    List<ProjectMember> findByProjectRole(ProjectMemberRole projectRole);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectId(Long projectId);

    long countByUserId(Long userId);

    long countByProjectRole(ProjectMemberRole projectRole);

    long countByProjectIdAndProjectRole(Long projectId, ProjectMemberRole projectRole);

    int deleteByProjectId(Long projectId);

    int deleteByUserId(Long userId);

    int deleteByProjectIdAndUserId(Long projectId, Long userId);

    @Query("SELECT pm.projectRole FROM ProjectMember pm WHERE pm.userId = :userId AND pm.projectId = :projectId")
    Optional<ProjectMemberRole> findUserRoleInProject(@Param("userId") Long userId,
                                                     @Param("projectId") Long projectId);

    @Query("SELECT pm.projectId FROM ProjectMember pm WHERE pm.userId = :userId")
    List<Long> findProjectIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT pm.userId FROM ProjectMember pm WHERE pm.projectId = :projectId")
    List<Long> findUserIdsByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.projectId IN :projectIds")
    List<ProjectMember> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);
}

