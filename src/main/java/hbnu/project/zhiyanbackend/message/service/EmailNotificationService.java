package hbnu.project.zhiyanbackend.message.service;

import hbnu.project.zhiyanbackend.message.model.entity.MessageBody;
import hbnu.project.zhiyanbackend.message.model.enums.MessagePriority;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;

/**
 * 邮件通知服务接口
 * 负责将高优先级的消息推送到用户邮箱
 *
 * @author ErgouTree
 */
public interface EmailNotificationService {

    /**
     * 发送邮件通知
     * 根据用户偏好设置，决定是否发送邮件
     *
     * @param userId 用户ID
     * @param messageBody 消息体
     */
    void sendEmailNotificationIfEnabled(Long userId, MessageBody messageBody);

    /**
     * 批量发送邮件通知
     *
     * @param userIds 用户ID列表
     * @param messageBody 消息体
     */
    void sendBatchEmailNotificationIfEnabled(java.util.List<Long> userIds, MessageBody messageBody);

    /**
     * 检查用户是否启用了某个场景的邮件通知
     *
     * @param userId 用户ID
     * @param scene 消息场景
     * @return 是否启用
     */
    boolean isEmailNotificationEnabled(Long userId, MessageScene scene);
}

