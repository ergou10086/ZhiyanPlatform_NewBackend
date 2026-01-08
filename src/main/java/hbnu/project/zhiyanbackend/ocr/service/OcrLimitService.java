package hbnu.project.zhiyanbackend.ocr.service;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.ocr.config.OcrLimitConfig;
import hbnu.project.zhiyanbackend.ocr.model.entity.UserOcrUsage;
import hbnu.project.zhiyanbackend.ocr.model.enums.OcrType;
import hbnu.project.zhiyanbackend.ocr.repository.UserOcrUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * OCR使用限制服务
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrLimitService {

    private final UserOcrUsageRepository userOcrUsageRepository;
    private final OcrLimitConfig ocrLimitConfig;

    /**
     * 检查用户是否可以使用指定的OCR类型
     * 如果超过限制则抛出异常
     *
     * @param userId  用户ID
     * @param ocrType OCR类型
     * @throws ServiceException 如果超过使用限制
     */
    public void checkUsageLimit(Long userId, OcrType ocrType) {
        // 如果限制功能未启用，直接返回
        if (!ocrLimitConfig.isEnabled()) {
            log.debug("OCR使用限制功能未启用，跳过检查");
            return;
        }

        if (userId == null || userId <= 0) {
            throw new ServiceException("用户未登录，无法使用OCR服务");
        }

        LocalDate today = LocalDate.now();
        Optional<UserOcrUsage> usageOpt = userOcrUsageRepository.findByUserIdAndOcrTypeAndUsageDate(userId, ocrType, today);

        int currentUsage = 0;
        if (usageOpt.isPresent()) {
            currentUsage = usageOpt.get().getUsageCount();
        }

        int dailyLimit = ocrType.getDailyLimit();
        if (currentUsage >= dailyLimit) {
            log.warn("用户 {} 使用 {} 已达到每日限制 {} 次", userId, ocrType.getDescription(), dailyLimit);
            throw new ServiceException(
                    String.format("您今日使用%s已达到上限（%d次），请明天再试", ocrType.getDescription(), dailyLimit)
            );
        }

        log.debug("用户 {} 使用 {} 检查通过，当前使用 {}/{} 次", userId, ocrType.getDescription(), currentUsage, dailyLimit);
    }

    /**
     * 记录用户使用OCR服务
     * 如果记录不存在则创建，存在则增加使用次数
     *
     * @param userId  用户ID
     * @param ocrType OCR类型
     */
    @Transactional
    public void recordUsage(Long userId, OcrType ocrType) {
        // 如果限制功能未启用，不记录
        if (!ocrLimitConfig.isEnabled()) {
            return;
        }

        if (userId == null || userId <= 0) {
            log.warn("尝试记录OCR使用，但用户ID无效: {}", userId);
            return;
        }

        LocalDate today = LocalDate.now();
        Optional<UserOcrUsage> usageOpt = userOcrUsageRepository.findByUserIdAndOcrTypeAndUsageDate(userId, ocrType, today);

        UserOcrUsage usage;
        if (usageOpt.isPresent()) {
            // 如果记录已存在，增加使用次数
            usage = usageOpt.get();
            usage.incrementUsageCount();
            log.debug("用户 {} 使用 {} 次数增加，当前: {} 次", userId, ocrType.getDescription(), usage.getUsageCount());
        } else {
            // 如果记录不存在，创建新记录
            usage = UserOcrUsage.builder()
                    .id(SnowflakeIdUtils.nextId())
                    .userId(userId)
                    .ocrType(ocrType)
                    .usageDate(today)
                    .usageCount(1)
                    .build();
            log.debug("用户 {} 首次使用 {}，创建使用记录", userId, ocrType.getDescription());
        }

        userOcrUsageRepository.save(usage);
    }

    /**
     * 获取用户今日指定OCR类型的使用次数
     *
     * @param userId  用户ID
     * @param ocrType OCR类型
     * @return 今日使用次数
     */
    public int getTodayUsageCount(Long userId, OcrType ocrType) {
        if (userId == null || userId <= 0) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        Optional<UserOcrUsage> usageOpt = userOcrUsageRepository.findByUserIdAndOcrTypeAndUsageDate(userId, ocrType, today);

        return usageOpt.map(UserOcrUsage::getUsageCount).orElse(0);
    }

    /**
     * 获取用户今日指定OCR类型的剩余使用次数
     *
     * @param userId  用户ID
     * @param ocrType OCR类型
     * @return 剩余使用次数
     */
    public int getRemainingUsageCount(Long userId, OcrType ocrType) {
        int todayUsage = getTodayUsageCount(userId, ocrType);
        int dailyLimit = ocrType.getDailyLimit();
        return Math.max(0, dailyLimit - todayUsage);
    }
}
