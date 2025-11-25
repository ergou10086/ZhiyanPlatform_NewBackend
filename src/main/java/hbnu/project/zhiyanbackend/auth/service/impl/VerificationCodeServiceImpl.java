package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.model.entity.VerificationCode;
import hbnu.project.zhiyanbackend.auth.model.enums.VerificationCodeType;
import hbnu.project.zhiyanbackend.auth.repository.VerificationCodeRepository;
import hbnu.project.zhiyanbackend.auth.service.MailService;
import hbnu.project.zhiyanbackend.auth.service.VerificationCodeService;
import hbnu.project.zhiyanbackend.auth.utils.VerificationCodeGenerator;
import hbnu.project.zhiyanbackend.basic.constants.CacheConstants;
import hbnu.project.zhiyanbackend.basic.domain.R;


import hbnu.project.zhiyanbackend.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;

    private final MailService mailService;

    private final RedisService redisService;

    // 验证码配置,从配置文件读取
    @Value("${app.verification-code.length:6}")
    private int CODE_LENGTH;

    @Value("${app.verification-code.expire-minutes:10}")
    private int CODE_EXPIRE_MINUTES;

    @Value("${app.verification-code.rate-limit-minutes:1}")
    private double RATE_LIMIT_MINUTES;

    @Value("${app.verification-code.enable-email-sending:true}")
    private boolean ENABLE_EMAIL_SENDING;


    /**
     * 生成并发送验证码
     *
     * @param email 邮箱地址
     * @param type 验证码类型
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> generateAndSendCode(String email, VerificationCodeType type) {
        try {
            // 检查发送频率限制
            if (!canSendCode(email, type)) {
                return R.fail("验证码发送过于频繁,请稍后再试");
            }

            // 生成验证码
            String code = VerificationCodeGenerator.generateNumericCode(CODE_LENGTH);

            log.info("验证码: {}", code);

            // 存入Redis缓存
            String redisKey = buildVerificationCodeKey(email, type);
            redisService.setCacheObject(redisKey, code, (long) CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 持久化到数据库
            VerificationCode verificationCode = VerificationCode.builder()
                    .email(email)
                    .code(code)
                    .type(type)
                    .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES))
                    .isUsed(false)
                    .build();
            verificationCodeRepository.save(verificationCode);

            // ========== 在控制台打印验证码（方便测试） ==========
            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║              📧 验证码已生成（测试模式）                  ║");
            log.info("╠══════════════════════════════════════════════════════════╣");
            log.info("║  邮箱: {}", String.format("%-48s", email) + "║");
            log.info("║  类型: {}", String.format("%-48s", type) + "║");
            log.info("║  验证码: 【{}】", String.format("%-44s", code) + "║");
            log.info("║  有效期: {} 分钟", String.format("%-44s", CODE_EXPIRE_MINUTES) + "║");
            log.info("╚══════════════════════════════════════════════════════════╝");

            // 发送验证码邮件,如果启用
            if (ENABLE_EMAIL_SENDING) {
                boolean emailSent = mailService.sendVerificationCode(email, code, type);
                if (!emailSent) {
                    log.warn("验证码邮件发送失败,但已保存到数据库 - 邮箱: {}, 类型: {}", email, type);
                    // 注意：即使邮件发送失败，验证码已经打印在控制台了，仍然可以使用
                }
            } else {
                log.info("📧 邮件发送已禁用，请在控制台查看验证码");
            }

            // 设置频率限制（转换分钟为秒）
            String rateLimitKey = buildRateLimitKey(email, type);
            long rateLimitSeconds = (long) (RATE_LIMIT_MINUTES * 60);
            redisService.setCacheObject(rateLimitKey, "1", rateLimitSeconds, TimeUnit.SECONDS);

            log.info("✅ 验证码发送成功 - 邮箱: {}, 类型: {}", email, type);
            return R.ok(null, "验证码发送成功");

        } catch (Exception e) {
            log.error("验证码发送失败 - 邮箱: {}, 类型: {}, 错误: {}", email, type, e.getMessage(), e);
            return R.fail("验证码发送失败,请稍后重试");
        }
    }


    /**
     * 验证验证码
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证码类型
     * @return 验证结果
     */
    @Override
    public R<Boolean> validateCode(String email, String code, VerificationCodeType type) {
        try {
            // 检查验证码是否已被使用
            String usedKey = buildUsedCodeKey(email, code, type);
            if (redisService.hasKey(usedKey)) {
                log.warn("验证码已被使用 - 邮箱: {}, 验证码: {}, 类型: {}", email, code, type);
                return R.ok(false, "验证码已被使用");
            }

            // 先从Redis验证
            String redisKey = buildVerificationCodeKey(email, type);
            String storedCode = redisService.getCacheObject(redisKey);

            // redis验证成果就成功
            if (storedCode != null && storedCode.equals(code)) {
                markCodeAsUsed(email, code, type);
                log.info("验证码验证成功(Redis) - 邮箱: {}, 类型: {}", email, type);
                return R.ok(true, "验证码验证成功");
            }

            // Redis没结果，再从数据库验证
            var optionalCode = verificationCodeRepository
                    .findByEmailAndCodeAndTypeAndIsUsedFalse(email, code, type);

            if (optionalCode.isPresent()) {
                VerificationCode verificationCode = optionalCode.get();

                if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
                    log.warn("验证码已过期 - 邮箱: {}, 类型: {}", email, type);
                    return R.ok(false, "验证码已过期");
                }

                verificationCode.setIsUsed(true);
                verificationCodeRepository.save(verificationCode);
                markCodeAsUsed(email, code, type);

                log.info("验证码验证成功(数据库) - 邮箱: {}, 类型: {}", email, type);
                return R.ok(true, "验证码验证成功");
            }

            log.warn("验证码验证失败 - 邮箱: {}, 验证码: {}, 类型: {}", email, code, type);
            return R.ok(false, "验证码错误或已过期");

        } catch (Exception e) {
            log.error("验证码验证异常 - 邮箱: {}, 类型: {}, 错误: {}", email, type, e.getMessage(), e);
            return R.fail("验证码验证失败,请稍后重试");
        }
    }

    /**
     * 检查指定邮箱和类型的验证码是否可以发送（频率限制检查）
     *
     * @param email 邮箱地址
     * @param type 验证码类型
     * @return true表示可以发送，false表示发送过于频繁
     */
    @Override
    public boolean canSendCode(String email, VerificationCodeType type) {
        String rateLimitKey = buildRateLimitKey(email, type);
        return !redisService.hasKey(rateLimitKey);
    }

    /**
     * 清理数据库中已过期的验证码
     * 删除所有过期时间早于当前时间且未使用的验证码
     */
    @Override
    @Transactional
    public void cleanExpiredCodes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = verificationCodeRepository.deleteExpiredCodes(now);
            log.info("清理过期验证码完成,删除数量: {}", deletedCount);
        } catch (Exception e) {
            log.error("清理过期验证码失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 标记验证码为已使用状态
     * 1. 更新数据库中的验证码状态
     * 2. 在Redis中标记该验证码已使用
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证码类型
     */
    @Override
    public void markCodeAsUsed(String email, String code, VerificationCodeType type) {
        try {
            var optionalCode = verificationCodeRepository
                    .findByEmailAndCodeAndTypeAndIsUsedFalse(email, code, type);

            if (optionalCode.isPresent()) {
                VerificationCode verificationCode = optionalCode.get();
                verificationCode.setIsUsed(true);
                verificationCodeRepository.save(verificationCode);
            }
        } catch (Exception e) {
            log.error("标记验证码为已使用失败 - 邮箱: {}, 类型: {}", email, type, e);
        }
    }

    /**
     * 定时清理过期验证码
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @ConditionalOnProperty(name = "app.verification-code.enable-cleanup-task", havingValue = "true", matchIfMissing = true)
    public void scheduledCleanupExpiredCodes() {
        log.info("========== 开始执行定时清理过期验证码任务 ==========");
        try {
            // 清理数据库中的过期验证码
            cleanExpiredCodes();

            // 清理数据库中24小时前已使用的验证码
            cleanUsedCodes();

            log.info("========== 定时清理过期验证码任务执行完成 ==========");
        } catch (Exception e) {
            log.error("定时清理过期验证码任务执行失败", e);
        }
    }

    /**
     * 清理数据库中24小时前已使用的验证码
     */
    @Transactional
    public void cleanUsedCodes() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
            int deletedCount = verificationCodeRepository.deleteUsedCodesBeforeTime(cutoffTime);
            log.info("清理已使用验证码完成，删除数量: {}", deletedCount);
        } catch (Exception e) {
            log.error("清理已使用验证码失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建验证码Redis缓存键
     *
     * @param email 邮箱地址
     * @param type 验证码类型
     * @return 组合后的Redis键
     */
    private String buildVerificationCodeKey(String email, VerificationCodeType type) {
        return CacheConstants.VERIFICATION_CODE_PREFIX + type.name().toLowerCase() + ":" + email;
    }

    /**
     * 构建频率限制Redis键
     *
     * @param email 邮箱地址
     * @param type 验证码类型
     * @return 组合后的Redis键
     */
    private String buildRateLimitKey(String email, VerificationCodeType type) {
        return CacheConstants.RATE_LIMIT_PREFIX + type.name().toLowerCase() + ":" + email;
    }

    /**
     * 构建已使用验证码标记Redis键
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证码类型
     * @return 组合后的Redis键
     */
    private String buildUsedCodeKey(String email, String code, VerificationCodeType type) {
        return CacheConstants.USED_CODE_PREFIX + type.name().toLowerCase() + ":" + email + ":" + code;
    }
}