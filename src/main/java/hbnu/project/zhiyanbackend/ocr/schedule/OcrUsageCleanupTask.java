package hbnu.project.zhiyanbackend.ocr.schedule;

import hbnu.project.zhiyanbackend.ocr.repository.UserOcrUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * OCR使用记录清理任务
 *
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrUsageCleanupTask {

    private final UserOcrUsageRepository userOcrUsageRepository;

    /**
     * 每天凌晨2点清理30天前的使用记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredRecords() {
        LocalDate expireDate = LocalDate.now().minusDays(30);
        int deletedCount = userOcrUsageRepository.deleteExpiredRecords(expireDate);
        log.info("清理OCR使用记录完成，删除 {} 条30天前的记录", deletedCount);
    }
}
