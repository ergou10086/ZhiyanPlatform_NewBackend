package hbnu.project.zhiyanbackend.auth.repository;

import hbnu.project.zhiyanbackend.auth.model.entity.RememberMeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * RememberMeToken数据访问接口
 *
 * @author ErgouTree
 */
@Repository
public interface RememberMeTokenRepository extends JpaRepository<RememberMeToken, Long> {
    
    /**
     * 根据token查找RememberMe token
     *
     * @param token token值
     * @return RememberMe token对象（可能为空）
     */
    Optional<RememberMeToken> findByToken(String token);
    
    /**
     * 根据用户ID删除RememberMe token
     *
     * @param userId 用户ID
     */
    @Modifying
    @Query("DELETE FROM RememberMeToken r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID查找RememberMe token
     *
     * @param userId 用户ID
     * @return RememberMe token对象（可能为空）
     */
    Optional<RememberMeToken> findByUserId(Long userId);
    
    /**
     * 删除过期的RememberMe token
     *
     * @param expiryTime 过期时间
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM RememberMeToken r WHERE r.expiryTime < :expiryTime")
    int deleteByExpiryTimeBefore(@Param("expiryTime") LocalDateTime expiryTime);
}

