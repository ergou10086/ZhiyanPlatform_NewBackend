package hbnu.project.zhiyanbackend.ocr.repository;

import hbnu.project.zhiyanbackend.ocr.model.entity.UserOcrUsage;
import hbnu.project.zhiyanbackend.ocr.model.enums.OcrType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 用户OCR使用记录仓库接口
 *
 * @author ErgouTree
 */
@Repository
public interface UserOcrUsageRepository extends JpaRepository<UserOcrUsage, Long> {

    /**
     * 根据用户ID、OCR类型和使用日期查找记录
     */
    Optional<UserOcrUsage> findByUserIdAndOcrTypeAndUsageDate(Long userId, OcrType ocrType, LocalDate usageDate);

    /**
     * 查找用户今日所有OCR类型使用记录
     */
    @Query("SELECT u FROM UserOcrUsage u WHERE u.userId = :userId AND u.usageDate = :today")
    List<UserOcrUsage> findAllByUserIdAndToday(
            @Param("userId") Long userId,
            @Param("today") LocalDate today);

    /**
     * 获取用户指定时间段内的使用记录
     */
    @Query("SELECT u FROM UserOcrUsage u WHERE u.userId = :userId AND u.usageDate BETWEEN :startDate AND :endDate")
    List<UserOcrUsage> findByUserIdAndUsageDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 删除过期的使用记录（保留最近30天）
     */
    @Modifying
    @Query("DELETE FROM UserOcrUsage u WHERE u.usageDate < :expireDate")
    int deleteExpiredRecords(@Param("expireDate") LocalDate expireDate);
}
