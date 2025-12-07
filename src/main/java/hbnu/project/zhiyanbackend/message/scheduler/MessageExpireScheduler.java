package hbnu.project.zhiyanbackend.message.scheduler;

import hbnu.project.zhiyanbackend.message.model.entity.MessageRecipient;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.message.repository.MessageRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息过期处理定时任务
 * 处理需要在规定时间内答复的消息(如邀请、申请等)
 *
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageExpireScheduler {

    private final MessageRecipientRepository messageRecipientRepository;

    /**
     * 每小时执行一次,检查并处理过期的邀请和申请消息
     * 邀请和申请消息在3天内未处理则自动失效
     */
    // 每小时整点执行
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredMessages() {
        log.info("开始处理过期消息...");

        try {
            // 计算3天前的时间点
            LocalDateTime expireTime = LocalDateTime.now().minusDays(3);

            // 查找过期的邀请消息
            int expiredInvitations = processExpiredInvitations(expireTime);

            // 查找过期的申请消息
            int expiredApplications = processExpiredApplications(expireTime);

            log.info("过期消息处理完成: 邀请消息{}条, 申请消息{}条",
                    expiredInvitations, expiredApplications);
        } catch (Exception e) {
            log.error("处理过期消息时发生错误", e);
        }
    }

    /**
     * 处理过期的邀请消息
     */
    private int processExpiredInvitations(LocalDateTime expireTime) {
        List<MessageRecipient> expiredInvitations = messageRecipientRepository
                .findExpiredUnreadMessages(
                        MessageScene.PROJECT_MEMBER_INVITED.name(),
                        expireTime
                );

        if (!expiredInvitations.isEmpty()) {
            for (MessageRecipient recipient : expiredInvitations) {
                // 标记为过期
                recipient.setExpired(true);
                recipient.setExpiredAt(LocalDateTime.now());

                // 更新消息内容,添加过期提示
                if (recipient.getMessageBody() != null) {
                    String originalContent = recipient.getMessageBody().getContent();
                    String expiredContent = originalContent + "\n\n【此邀请已过期】\n该邀请超过3天未处理,已自动失效。";
                    recipient.getMessageBody().setContent(expiredContent);
                }
            }
            messageRecipientRepository.saveAll(expiredInvitations);
            log.info("标记{}条过期的邀请消息", expiredInvitations.size());
        }

        return expiredInvitations.size();
    }

    /**
     * 处理过期的申请消息
     */
    private int processExpiredApplications(LocalDateTime expireTime) {
        List<MessageRecipient> expiredApplications = messageRecipientRepository
                .findExpiredUnreadMessages(
                        MessageScene.PROJECT_MEMBER_APPLY.name(),
                        expireTime
                );

        if (!expiredApplications.isEmpty()) {
            for (MessageRecipient recipient : expiredApplications) {
                // 标记为已过期
                recipient.setExpired(true);
                recipient.setExpiredAt(LocalDateTime.now());

                // 更新消息内容,添加过期提示
                if (recipient.getMessageBody() != null) {
                    String originalContent = recipient.getMessageBody().getContent();
                    String expiredContent = originalContent + "\n\n【此申请已过期】\n该申请超过3天未处理,已自动失效。";
                    recipient.getMessageBody().setContent(expiredContent);
                }
            }
            messageRecipientRepository.saveAll(expiredApplications);
            log.info("标记{}条过期的申请消息", expiredApplications.size());
        }

        return expiredApplications.size();
    }
}
