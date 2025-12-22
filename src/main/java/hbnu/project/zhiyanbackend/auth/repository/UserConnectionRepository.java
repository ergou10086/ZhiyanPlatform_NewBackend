package hbnu.project.zhiyanbackend.auth.repository;

import hbnu.project.zhiyanbackend.auth.model.entity.UserConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户第三方账号绑定关系数据访问层
 *
 * @author ErgouTree
 */
@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnection, Long> {

    /**
     * 根据提供商和提供商用户ID查找绑定关系
     * 这是最核心的查询：用于判断第三方账号是否已绑定
     *
     * @param provider       提供商名称
     * @param providerUserId 提供商用户ID
     * @return 绑定关系
     */
    Optional<UserConnection> findByProviderAndProviderUserIdAndIsUnboundFalse(
            String provider, String providerUserId);

    /**
     * 查询用户的所有绑定关系
     *
     * @param userId 用户ID
     * @return 绑定关系列表
     */
    List<UserConnection> findByUserIdAndIsUnboundFalse(Long userId);

    /**
     * 查询用户在某个提供商的绑定关系
     *
     * @param userId   用户ID
     * @param provider 提供商名称
     * @return 绑定关系
     */
    Optional<UserConnection> findByUserIdAndProviderAndIsUnboundFalse(
            Long userId, String provider);

    /**
     * 检查某个第三方账号是否已被绑定
     *
     * @param provider       提供商名称
     * @param providerUserId 提供商用户ID
     * @return 是否存在
     */
    boolean existsByProviderAndProviderUserIdAndIsUnboundFalse(
            String provider, String providerUserId);

    /**
     * 检查用户是否已绑定某个提供商
     *
     * @param userId   用户ID
     * @param provider 提供商名称
     * @return 是否存在
     */
    boolean existsByUserIdAndProviderAndIsUnboundFalse(Long userId, String provider);

    /**
     * 统计用户绑定的第三方账号数量
     *
     * @param userId 用户ID
     * @return 绑定数量
     */
    @Query("SELECT COUNT(uc) FROM UserConnection uc WHERE uc.userId = :userId AND uc.isUnbound = false")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 根据提供商邮箱查找可能的绑定关系
     * 用于邮箱匹配策略
     *
     * @param providerEmail 提供商邮箱
     * @return 绑定关系列表
     */
    List<UserConnection> findByProviderEmailAndIsUnboundFalse(String providerEmail);
}
