package hbnu.project.zhiyanbackend.message.service.impl;


import hbnu.project.zhiyanbackend.message.model.entity.MessageBody;
import hbnu.project.zhiyanbackend.message.model.entity.MessageRecipient;
import hbnu.project.zhiyanbackend.message.model.entity.MessageSendRecord;
import hbnu.project.zhiyanbackend.message.model.enums.MessagePriority;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.message.model.enums.MessageType;
import hbnu.project.zhiyanbackend.message.repository.MessageBodyRepository;
import hbnu.project.zhiyanbackend.message.repository.MessageRecipientRepository;
import hbnu.project.zhiyanbackend.message.repository.MessageSendRecordRepository;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.message.utils.SseMessagePushEdgeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 站内消息服务类实现
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxMessageServiceImpl implements InboxMessageService {

    private final MessageBodyRepository messageBodyRepository;
    private final MessageRecipientRepository messageRecipientRepository;
    private final MessageSendRecordRepository messageSendRecordRepository;
    private final SseMessagePushEdgeUtils sseMessagePushEdgeUtils;

    /**
     * 发送一条“个人消息”（向单个的收件人发送）
     *
     * @param scene          消息场景
     * @param senderId       发送人ID（系统消息可为 null）
     * @param receiverId     收件人ID
     * @param title          标题
     * @param content        正文
     * @param businessId     业务ID（任务ID、项目ID等，可为空）
     * @param businessType   业务类型（"TASK" / "PROJECT" / "ACHIEVEMENT" 等，可为空）
     * @param extendDataJson 扩展字段 JSON（跳转链接、额外参数等）
     * @return MessageBody（便于调用方根据需要做调试或链路跟踪）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageBody sendPersonalMessage(MessageScene scene, Long senderId, Long receiverId, String title, String content, Long businessId, String businessType, String extendDataJson) {
        // 构建消息体


        MessageBody messageBody = MessageBody.builder()
                .senderId(senderId)
                .messageType(MessageType.PERSONAL)
                .scene(scene)
                .priority(MessagePriority.ofScene(scene))
                .title(title)
                .content(content)
                .businessId(businessId)
                .businessType(businessType)
                .extendData(extendDataJson)
                .triggerTime(LocalDateTime.now())
                .build();

        // 构建收件人记录
        MessageRecipient recipient = MessageRecipient.builder()
                .receiverId(receiverId)
                .sceneCode(scene.name())
                .readFlag(false)
                .deleted(false)
                .triggerTime(messageBody.getTriggerTime())
                .build();

        messageBody.addRecipient(recipient);

        // 持久化消息
        messageBody = messageBodyRepository.save(messageBody);

        // 通过SSE推送消息
        try {
            sseMessagePushEdgeUtils.pushMessageViaSse(receiverId, messageBody);
        } catch (Exception e) {
            log.error("SSE推送消息失败,但消息已保存: receiverId={}, messageBodyId={}",
                    receiverId, messageBody.getId(), e);
        }

        log.info("发送个人消息成功: messageBodyId={}, receiverId={}, scene={}",
                messageBody.getId(), receiverId, scene);

        return messageBody;
    }


    /**
     * 发送多收件人消息(多收件人,一条消息体,多条收件人记录)
     *
     * @param scene          消息场景
     * @param senderId       发送人ID
     * @param receiverIds    收件人ID集合
     * @param title          标题
     * @param content        正文
     * @param businessId     业务ID
     * @param businessType   业务类型
     * @param extendDataJson 扩展数据
     * @return MessageBody
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageBody sendBatchPersonalMessage(MessageScene scene, Long senderId,
                                                Collection<Long> receiverIds, String title,
                                                String content, Long businessId,
                                                String businessType, String extendDataJson) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            log.warn("批量发送消息失败: 收件人列表为空");
            return null;
        }

        // 构建消息体
        MessageBody messageBody = MessageBody.builder()
                .senderId(senderId)
                .messageType(MessageType.PERSONAL)
                .scene(scene)
                .priority(MessagePriority.ofScene(scene))
                .title(title)
                .content(content)
                .businessId(businessId)
                .businessType(businessType)
                .extendData(extendDataJson)
                .triggerTime(LocalDateTime.now())
                .build();

        // 批量创建收件人记录
        MessageBody finalMessageBody = messageBody;
        List<MessageRecipient> recipients = receiverIds.stream()
                .map(receiverId -> MessageRecipient.builder()
                        .receiverId(receiverId)
                        .sceneCode(scene.name())
                        .readFlag(false)
                        .deleted(false)
                        .triggerTime(finalMessageBody.getTriggerTime())
                        .build())
                .collect(Collectors.toList());

        messageBody.addRecipients(recipients);

        // 保存消息
        messageBody = messageBodyRepository.save(messageBody);

        // 创建发送记录
        LocalDateTime now = LocalDateTime.now();
        MessageSendRecord sendRecord = MessageSendRecord.builder()
                .messageBodyId(messageBody.getId())
                .sendTime(now)
                .totalRecipients(receiverIds.size())
                .successCount(0)
                .failedCount(0)
                .status("SENDING")
                .createdAt(now)  // 设置创建时间
                .createdBy(senderId) // 设置创建人
                .updatedAt(now)  // 设置更新时间
                .updatedBy(senderId) // 设置更新人
                .version(0) // 初始化版本号
                .build();
        messageSendRecordRepository.save(sendRecord);

        // 通过SSE批量推送消息
        int successCount = 0;
        int failedCount = 0;
        for (Long receiverId : receiverIds) {
            try {
                sseMessagePushEdgeUtils.pushMessageViaSse(receiverId, messageBody);
                successCount++;
            } catch (Exception e) {
                log.error("SSE推送消息失败: receiverId={}, messageBodyId={}",
                        receiverId, messageBody.getId(), e);
                failedCount++;
            }
        }

        // 更新发送记录
        sendRecord.setSuccessCount(successCount);
        sendRecord.setFailedCount(failedCount);
        sendRecord.setStatus(failedCount == 0 ? "SUCCESS" :
                (successCount > 0 ? "PARTIAL_FAILED" : "ALL_FAILED"));
        messageSendRecordRepository.save(sendRecord);

        log.info("批量发送消息完成: messageBodyId={}, total={}, success={}, failed={}",
                messageBody.getId(), receiverIds.size(), successCount, failedCount);

        return messageBody;
    }


    /**
     * 向全体用户发送消息
     * 向全体用户发送消息只能是管理员发送
     * 这种消息类型通常只有系统消息，不用管理业务模块
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageBody sendAllPersonalMessage(MessageScene scene, Long senderId, String title, String content, String extendDataJson) {
        // 创建广播消息体
        MessageBody messageBody = MessageBody.builder()
                .senderId(senderId)
                .messageType(MessageType.BROADCAST)
                .scene(scene)
                .priority(MessagePriority.ofScene(scene))
                .title(title)
                .content(content)
                .extendData(extendDataJson)
                .triggerTime(LocalDateTime.now())
                .build();

        // 广播消息不创建收件人记录，通过SSE直接推送
        messageBody = messageBodyRepository.save(messageBody);

        // 通过SSE广播消息
        try {
            sseMessagePushEdgeUtils.pushBroadcastMessageViaSse(messageBody);
        } catch (Exception e) {
            log.error("SSE广播消息失败,但消息已保存: messageBodyId={}", messageBody.getId(), e);
        }

        log.info("发送广播消息成功: messageBodyId={}, scene={}",
                messageBody.getId(), scene);

        return messageBody;
    }


    /**
     * 将某条消息标记为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long receiverId, Long recipientId) {
        Optional<MessageRecipient> messageRecipientOptional = messageRecipientRepository.findByIdAndReceiverIdAndDeletedFalse(recipientId, receiverId);

        if (messageRecipientOptional.isPresent()) {
            MessageRecipient messageRecipient = messageRecipientOptional.get();
            if(!messageRecipient.getReadFlag()){
                messageRecipient.markAsRead();
                messageRecipientRepository.save(messageRecipient);
                log.info("标记消息已读: recipientId={}, receiverId={}", recipientId, receiverId);
            }
        }else {
            log.warn("标记消息已读失败: 消息不存在或已删除, recipientId={}, receiverId={}",
                    recipientId, receiverId);
        }
    }


    /**
     * 将当前用户全部未读消息标记为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long receiverId) {
        List<MessageRecipient> unreadList = messageRecipientRepository.findByReceiverIdAndReadFlagFalseAndDeletedFalse(receiverId);

        unreadList.forEach(MessageRecipient::markAsRead);
        messageRecipientRepository.saveAll(unreadList);

        log.info("全部标记已读: receiverId={}, count={}", receiverId, unreadList.size());
    }


    /**
     * 删除某条消息（真删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long receiverId, Long recipientId) {
        Optional<MessageRecipient> optional = messageRecipientRepository.findByIdAndReceiverIdAndDeletedFalse(recipientId, receiverId);

        if (optional.isPresent()) {
            MessageRecipient messageRecipient = optional.get();
            Long messageBodyId = messageRecipient.getMessageBodyId();
            
            // 真删除收件人记录
            messageRecipientRepository.delete(messageRecipient);
            log.info("删除消息: recipientId={}, receiverId={}, messageBodyId={}", 
                    recipientId, receiverId, messageBodyId);
            
            // 检查该消息体是否还有其他收件人，如果没有则删除消息体
            long remainingRecipients = messageRecipientRepository.countByMessageBodyId(messageBodyId);
            if (remainingRecipients == 0) {
                messageBodyRepository.deleteById(messageBodyId);
                log.info("消息体已无收件人，删除消息体: messageBodyId={}", messageBodyId);
            }
        } else {
            log.warn("删除消息失败: 消息不存在或已删除, recipientId={}, receiverId={}",
                    recipientId, receiverId);
        }
    }


    /**
     * 清空当前用户的所有已读消息（真删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllReadMessage(Long receiverId) {
        // 查询所有已读且未删除的消息
        List<MessageRecipient> readList = messageRecipientRepository
                .findByReceiverIdAndReadFlagTrueAndDeletedFalse(receiverId);

        if (readList.isEmpty()) {
            log.info("清空已读消息: receiverId={}, count=0 (没有已读消息)", receiverId);
            return;
        }

        // 收集所有相关的消息体ID，用于后续检查是否需要删除消息体
        List<Long> messageBodyIds = readList.stream()
                .map(MessageRecipient::getMessageBodyId)
                .distinct()
                .collect(Collectors.toList());

        // 真删除所有已读消息的收件人记录
        messageRecipientRepository.deleteAll(readList);
        log.info("清空已读消息: receiverId={}, deletedCount={}", receiverId, readList.size());

        // 检查并删除没有收件人的消息体
        for (Long messageBodyId : messageBodyIds) {
            long remainingRecipients = messageRecipientRepository.countByMessageBodyId(messageBodyId);
            if (remainingRecipients == 0) {
                messageBodyRepository.deleteById(messageBodyId);
                log.debug("消息体已无收件人，删除消息体: messageBodyId={}", messageBodyId);
            }
        }
    }


    /**
     * 用户收件箱分页查询（全部）
     * 自己隔这二级构建一下这个方法
     */
    @Override
    public Page<MessageRecipient> pageInbox(Long receiverId, Pageable pageable) {
        return messageRecipientRepository.findByReceiverIdAndDeletedFalseOrderByTriggerTimeDesc(
                receiverId, pageable);
    }


    /**
     * 用户未读消息分页查询
     */
    @Override
    public Page<MessageRecipient> pageUnread(Long receiverId, Pageable pageable) {
        return messageRecipientRepository.findByReceiverIdAndReadFlagFalseAndDeletedFalseOrderByTriggerTimeDesc(
                receiverId, pageable);
    }


    /**
     * 未读数量
     */
    @Override
    public long countUnread(Long receiverId) {
        return messageRecipientRepository.countByReceiverIdAndReadFlagFalseAndDeletedFalse(receiverId);
    }
}
