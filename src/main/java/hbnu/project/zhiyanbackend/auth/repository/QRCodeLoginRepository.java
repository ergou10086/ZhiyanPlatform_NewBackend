package hbnu.project.zhiyanbackend.auth.repository;

import hbnu.project.zhiyanbackend.auth.model.entity.QRCodeLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 扫码登录Repository层
 *
 * @author ErgouTree
 */
public interface QRCodeLoginRepository extends JpaRepository<QRCodeLogin, Long> {

    /**
     * 根据二维码ID查询
     */
    Optional<QRCodeLogin> findByQrCode(String qrCode);

    /**
     * 删除过期的二维码记录
     */
    int deleteByExpireTimeBefore(LocalDateTime expireTime);
}
