package hbnu.project.zhiyanbackend.auth.schedule;

import hbnu.project.zhiyanbackend.auth.service.QRCodeLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 二维码清理定时任务
 * 定期清理过期的二维码登录记录
 *
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QRCodeCleanupScheduleTask {

    private final QRCodeLoginService qrCodeLoginService;

    @Scheduled(cron = "0 */10 * * * ?")
    public void cleanExpiredQRCodeLogins() {
        try {
            qrCodeLoginService.cleanExpiredQRCodes();
        } catch (Exception e) {
            log.error("清理过期二维码失败", e);
        }
    }
}
