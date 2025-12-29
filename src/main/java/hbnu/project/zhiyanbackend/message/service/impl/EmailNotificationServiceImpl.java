package hbnu.project.zhiyanbackend.message.service.impl;

import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.utils.MailUtils;
import hbnu.project.zhiyanbackend.message.model.entity.MessageBody;
import hbnu.project.zhiyanbackend.message.model.entity.UserEmailNotificationPreference;
import hbnu.project.zhiyanbackend.message.model.enums.MessagePriority;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.message.repository.UserEmailNotificationPreferenceRepository;
import hbnu.project.zhiyanbackend.message.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 邮件通知服务实现
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final UserEmailNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    public void sendEmailNotificationIfEnabled(Long userId, MessageBody messageBody) {
        if (userId == null || messageBody == null) {
            return;
        }

        // 只处理高优先级的消息
        if (messageBody.getPriority() != MessagePriority.HIGH) {
            return;
        }

        // 检查用户是否启用了该场景的邮件通知
        if (!isEmailNotificationEnabled(userId, messageBody.getScene())) {
            log.debug("用户[{}]未启用场景[{}]的邮件通知，跳过发送", userId, messageBody.getScene());
            return;
        }

        // 获取用户信息
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户[{}]不存在，无法发送邮件通知", userId);
            return;
        }

        User user = userOpt.get();
        if (!StringUtils.hasText(user.getEmail())) {
            log.debug("用户[{}]未设置邮箱，无法发送邮件通知", userId);
            return;
        }

        try {
            // 生成邮件内容
            String emailSubject = generateEmailSubject(messageBody);
            String emailContent = generateEmailContent(user, messageBody);

            // 发送邮件
            MailUtils.sendHtml(user.getEmail(), emailSubject, emailContent);
            log.info("邮件通知发送成功: userId={}, scene={}, messageId={}", 
                    userId, messageBody.getScene(), messageBody.getId());
        } catch (Exception e) {
            log.error("发送邮件通知失败: userId={}, scene={}, messageId={}", 
                    userId, messageBody.getScene(), messageBody.getId(), e);
            // 邮件发送失败不影响主流程
        }
    }

    @Override
    public void sendBatchEmailNotificationIfEnabled(List<Long> userIds, MessageBody messageBody) {
        if (userIds == null || userIds.isEmpty() || messageBody == null) {
            return;
        }

        // 只处理高优先级的消息
        if (messageBody.getPriority() != MessagePriority.HIGH) {
            return;
        }

        for (Long userId : userIds) {
            try {
                sendEmailNotificationIfEnabled(userId, messageBody);
            } catch (Exception e) {
                log.error("批量发送邮件通知失败: userId={}", userId, e);
                // 继续处理下一个用户
            }
        }
    }

    @Override
    public boolean isEmailNotificationEnabled(Long userId, MessageScene scene) {
        if (userId == null || scene == null) {
            return false;
        }

        Optional<UserEmailNotificationPreference> preferenceOpt = 
                preferenceRepository.findByUserId(userId);
        
        if (preferenceOpt.isEmpty()) {
            // 默认不启用
            return false;
        }

        UserEmailNotificationPreference preference = preferenceOpt.get();
        return preference.isSceneEnabled(scene.name());
    }

    /**
     * 生成邮件主题
     */
    private String generateEmailSubject(MessageBody messageBody) {
        String sceneDesc = messageBody.getScene().getDesc();
        return String.format("【智研平台】%s", sceneDesc);
    }

    /**
     * 生成邮件内容（HTML格式）
     */
    private String generateEmailContent(User user, MessageBody messageBody) {
        StringBuilder html = new StringBuilder();
        
        // HTML头部
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"zh-CN\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>").append(generateEmailSubject(messageBody)).append("</title>");
        html.append("<style>");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; ");
        html.append("line-height: 1.6; color: #333; background-color: #f5f5f5; margin: 0; padding: 0; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 0; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; ");
        html.append("padding: 30px 20px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 24px; font-weight: 600; }");
        html.append(".content { padding: 30px 20px; }");
        html.append(".greeting { font-size: 16px; margin-bottom: 20px; }");
        html.append(".message-box { background-color: #f8f9fa; border-left: 4px solid #667eea; ");
        html.append("padding: 20px; margin: 20px 0; border-radius: 4px; }");
        html.append(".message-title { font-size: 18px; font-weight: 600; color: #667eea; margin-bottom: 15px; }");
        html.append(".message-content { font-size: 14px; color: #555; white-space: pre-wrap; line-height: 1.8; }");
        html.append(".message-meta { margin-top: 20px; padding-top: 20px; border-top: 1px solid #e0e0e0; ");
        html.append("font-size: 12px; color: #999; }");
        html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; ");
        html.append("font-size: 12px; color: #999; border-top: 1px solid #e0e0e0; }");
        html.append(".footer a { color: #667eea; text-decoration: none; }");
        html.append(".priority-badge { display: inline-block; background-color: #ff4757; color: #ffffff; ");
        html.append("padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; margin-left: 10px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        // 容器开始
        html.append("<div class=\"container\">");
        
        // 头部
        html.append("<div class=\"header\">");
        html.append("<h1>智研平台消息通知</h1>");
        html.append("</div>");
        
        // 内容区域
        html.append("<div class=\"content\">");
        
        // 问候语
        html.append("<div class=\"greeting\">");
        html.append("尊敬的 ").append(escapeHtml(user.getName() != null ? user.getName() : "用户")).append("，您好！");
        html.append("</div>");
        
        // 消息框
        html.append("<div class=\"message-box\">");
        html.append("<div class=\"message-title\">");
        html.append(escapeHtml(messageBody.getScene().getDesc()));
        html.append("<span class=\"priority-badge\">高优先级</span>");
        html.append("</div>");
        html.append("<div class=\"message-content\">");
        html.append(escapeHtml(messageBody.getContent() != null ? messageBody.getContent() : ""));
        html.append("</div>");
        html.append("</div>");
        
        // 消息元信息
        html.append("<div class=\"message-meta\">");
        html.append("<p><strong>消息类型：</strong>").append(escapeHtml(messageBody.getScene().getDesc())).append("</p>");
        if (messageBody.getTriggerTime() != null) {
            html.append("<p><strong>触发时间：</strong>")
                .append(messageBody.getTriggerTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("</p>");
        }
        html.append("</div>");
        
        // 内容区域结束
        html.append("</div>");
        
        // 页脚
        html.append("<div class=\"footer\">");
        html.append("<p>此邮件由智研平台自动发送，请勿回复。</p>");
        html.append("<p>您可以在<a href=\"#\">个人设置</a>中管理邮件通知偏好。</p>");
        html.append("<p style=\"margin-top: 10px; color: #bbb;\">");
        html.append("© ").append(LocalDateTime.now().getYear()).append(" 智研平台 保留所有权利");
        html.append("</p>");
        html.append("</div>");
        
        // 容器结束
        html.append("</div>");
        
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

