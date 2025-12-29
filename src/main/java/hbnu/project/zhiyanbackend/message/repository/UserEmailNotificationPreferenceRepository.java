package hbnu.project.zhiyanbackend.message.repository;

import hbnu.project.zhiyanbackend.message.model.entity.UserEmailNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户邮件通知偏好设置Repository
 *
 * @author ErgouTree
 */
@Repository
public interface UserEmailNotificationPreferenceRepository extends JpaRepository<UserEmailNotificationPreference, Long> {

    /**
     * 根据用户ID查找偏好设置
     *
     * @param userId 用户ID
     * @return 偏好设置
     */
    Optional<UserEmailNotificationPreference> findByUserId(Long userId);
}

