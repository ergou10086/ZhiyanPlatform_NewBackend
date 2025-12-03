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
 * 该接口继承自JpaRepository，提供了对ProjectMember实体的基本CRUD操作
 *
 * @author Tokito
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /**
     * 根据项目ID和用户ID查询项目成员
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 包含项目成员的Optional对象，如果不存在则为空
     */
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * 分页查询指定项目的所有成员
     * @param projectId 项目ID
     * @param pageable 分页参数
     * @return 分页的项目成员列表
     */
    Page<ProjectMember> findByProjectId(Long projectId, Pageable pageable);

    /**
     * 查询指定项目的所有成员
     * @param projectId 项目ID
     * @return 项目成员列表
     */
    List<ProjectMember> findByProjectId(Long projectId);

    /**
     * 查询用户参与的所有项目
     * @param userId 用户ID
     * @return 用户参与的项目列表
     */
    List<ProjectMember> findByUserId(Long userId);

    Page<ProjectMember> findByUserId(Long userId, Pageable pageable);
    /**
     * 分页查询用户参与的所有项目
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页的用户项目列表
     */

    List<ProjectMember> findByProjectIdAndProjectRole(Long projectId, ProjectMemberRole projectRole);
    /**
     * 查询指定项目中特定角色的所有成员
     * @param projectId 项目ID
     * @param projectRole 项目成员角色
     * @return 符合条件的成员列表
     */

    List<ProjectMember> findByProjectRole(ProjectMemberRole projectRole);
    /**
     * 查询所有具有特定角色的项目成员
     * @param projectRole 项目成员角色
     * @return 具有特定角色的成员列表
     */

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    /**
     * 检查指定项目中是否存在特定用户
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 如果存在则返回true，否则返回false
     */

    long countByProjectId(Long projectId);
    /**
     * 统计指定项目的成员数量
     * @param projectId 项目ID
     * @return 项目成员数量
     */

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

