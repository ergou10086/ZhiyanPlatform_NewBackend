package hbnu.project.zhiyanbackend.auth.repository;

import hbnu.project.zhiyanbackend.auth.model.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户成果关联 Repository
 *
 * @author ErgouTree
 */
@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    /**
     * 查询用户关联的所有成果（按展示顺序排序）
     *
     * @param userId 用户ID
     * @return 用户成果关联列表
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.userId = :userId ORDER BY ua.displayOrder ASC")
    List<UserAchievement> findByUserIdOrderByDisplayOrderAsc(@Param("userId") Long userId);

    /**
     * 查询用户在指定项目下的成果关联
     *
     * @param userId 用户ID
     * @param projectId 项目ID
     * @return 用户成果关联列表
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.userId = :userId AND ua.projectId = :projectId ORDER BY ua.displayOrder ASC")
    List<UserAchievement> findByUserIdAndProjectIdOrderByDisplayOrderAsc(@Param("userId") Long userId, @Param("projectId") Long projectId);

    /**
     * 检查用户是否已关联某成果
     *
     * @param userId 用户ID
     * @param achievementId 成果ID
     * @return 是否存在
     */
    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    /**
     * 查询用户对某成果的关联记录
     *
     * @param userId 用户ID
     * @param achievementId 成果ID
     * @return 用户成果关联对象（可能为空）
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.userId = :userId AND ua.achievementId = :achievementId")
    Optional<UserAchievement> findByUserIdAndAchievementId(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /**
     * 删除用户对某成果的关联
     *
     * @param userId 用户ID
     * @param achievementId 成果ID
     */
    @Modifying
    @Query("DELETE FROM UserAchievement ua WHERE ua.userId = :userId AND ua.achievementId = :achievementId")
    void deleteByUserIdAndAchievementId(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /**
     * 统计用户关联的成果数量
     *
     * @param userId 用户ID
     * @return 成果数量
     */
    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 查询关联了某成果的所有用户
     *
     * @param achievementId 成果ID
     * @return 用户成果关联列表
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.achievementId = :achievementId")
    List<UserAchievement> findByAchievementId(@Param("achievementId") Long achievementId);
}

