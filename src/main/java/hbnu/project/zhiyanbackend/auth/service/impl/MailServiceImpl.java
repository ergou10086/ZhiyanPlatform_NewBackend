package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.model.enums.VerificationCodeType;
import hbnu.project.zhiyanbackend.auth.service.MailService;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 邮件服务实现类
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:zhiyan163verif@163.com}")
    private String fromEmail;

    @Value("${app.name:智研平台}")
    private String appName;

    private static final int CODE_EXPIRE_MINUTES = 10;

    @Override
    public boolean sendVerificationCode(String toEmail, String code, VerificationCodeType type) {
        try {
            String subject = buildEmailSubject(type);
            String htmlContent = buildVerificationCodeHtmlTemplate(code,type);

            return sendHtmlMail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            log.error("发送验证码邮件失败 - 收件人: {}, 类型: {}, 错误: {}", toEmail, type, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendTextMail(String toEmail, String subject, String content) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(mimeMessage);
            log.info("文本邮件发送成功 - 收件人: {}, 主题: {}", toEmail, subject);
            return true;
        } catch (Exception e) {
            log.error("文本邮件发送失败 - 收件人: {}, 主题: {}, 错误: {}", toEmail, subject, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendHtmlMail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("HTML邮件发送成功 - 收件人: {}, 主题: {}", toEmail, subject);
            return true;
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败 - 收件人: {}, 主题: {}, 错误: {}", toEmail, subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建邮件主题
     */
    private String buildEmailSubject(VerificationCodeType type) {
        switch (type) {
            case REGISTER:
                return appName + " - 注册验证码";
            case RESET_PASSWORD:
                return appName + " - 密码重置验证码";
            case CHANGE_EMAIL:
                return appName + " - 邮箱变更验证码";
            default:
                return appName + " - 验证码";
        }
    }

    /**
     * 构建验证码HTML邮件模板
     * 蓝白渐变背景，居中显示验证码，包含logo
     *
     * @param code 验证码
     * @param type 验证码类型
     * @return HTML内容
     */
    private String buildVerificationCodeHtmlTemplate(String code, VerificationCodeType type) {
        String action;
        switch (type) {
            case REGISTER:
                action = "注册账户";
                break;
            case RESET_PASSWORD:
                action = "重置密码";
                break;
            case CHANGE_EMAIL:
                action = "变更邮箱";
                break;
            default:
                action = "身份验证";
                break;
        }

        String logoBase64 = getLogoPngBase64();

        return String.format("""
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - 验证码</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); min-height: 100vh;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse; border-spacing: 0; background: linear-gradient(135deg, #e0f2fe 0%%, #bae6fd 50%%, #7dd3fc 100%%); min-height: 100vh;">
                    <tr>
                        <td align="center" style="padding: 40px 20px;">
                            <table role="presentation" style="width: 100%%; max-width: 600px; background: #ffffff; border-radius: 16px; box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1); overflow: hidden;">
                                <!-- Logo区域 -->
                                <tr>
                                    <td align="center" style="padding: 40px 20px 20px 20px; background: linear-gradient(135deg, #3b82f6 0%%, #2563eb 100%%);">
                                        <div style="width: 120px; height: 120px; margin: 0 auto; background: #ffffff; border-radius: 50%%; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); padding: 10px;">
                                            <img src="%s" alt="%s Logo" style="width: 100px; height: 100px; display: block;" />
                                        </div>
                                    </td>
                                </tr>
                                
                                <!-- 标题区域 -->
                                <tr>
                                    <td align="center" style="padding: 30px 20px 10px 20px;">
                                        <h1 style="margin: 0; color: #1e293b; font-size: 28px; font-weight: 600; letter-spacing: -0.5px;">
                                            %s验证码
                                        </h1>
                                    </td>
                                </tr>
                                
                                <!-- 提示文字 -->
                                <tr>
                                    <td align="center" style="padding: 10px 20px 30px 20px;">
                                        <p style="margin: 0; color: #64748b; font-size: 16px; line-height: 1.6;">
                                            您正在进行<strong style="color: #2563eb;">%s</strong>操作，请使用以下验证码完成验证
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- 验证码区域 -->
                                <tr>
                                    <td align="center" style="padding: 0 20px 40px 20px;">
                                        <div style="background: linear-gradient(135deg, #3b82f6 0%%, #2563eb 100%%); border-radius: 12px; padding: 30px; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);">
                                            <div style="font-size: 42px; font-weight: 700; color: #ffffff; letter-spacing: 8px; text-align: center; font-family: 'Courier New', monospace; text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);">
                                                %s
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                
                                <!-- 有效期提示 -->
                                <tr>
                                    <td align="center" style="padding: 0 20px 30px 20px;">
                                        <p style="margin: 0; color: #94a3b8; font-size: 14px; line-height: 1.5;">
                                            ⏰ 验证码有效期为 <strong style="color: #ef4444;">%d分钟</strong>，请及时使用
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- 安全提示 -->
                                <tr>
                                    <td align="center" style="padding: 0 20px 40px 20px;">
                                        <div style="background: #f8fafc; border-left: 4px solid #3b82f6; border-radius: 4px; padding: 16px; margin: 0;">
                                            <p style="margin: 0; color: #64748b; font-size: 13px; line-height: 1.6;">
                                                🔒 如果这不是您的操作，请立即忽略此邮件并修改您的账户密码
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                                
                                <!-- 底部信息 -->
                                <tr>
                                    <td align="center" style="padding: 30px 20px; background: #f8fafc; border-top: 1px solid #e2e8f0;">
                                        <p style="margin: 0 0 8px 0; color: #94a3b8; font-size: 12px;">
                                            %s团队
                                        </p>
                                        <p style="margin: 0; color: #cbd5e1; font-size: 11px;">
                                            此邮件由系统自动发送，请勿回复
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
                appName,
                logoBase64,
                appName,
                action,
                action,
                code,
                CODE_EXPIRE_MINUTES,
                appName
        );
    }

    /**
     * 构建简化的Logo SVG（内联，兼容性更好）
     * 使用智研平台的蓝色主题色
     */
    private String buildSimplifiedLogoSvg() {
        // 使用简化的SVG logo，包含"智研"文字和图标元素
        return """
            <svg width="80" height="80" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
                <!-- 背景圆形 -->
                <circle cx="50" cy="50" r="48" fill="#2563eb" opacity="0.1"/>
                <!-- 主图标 - 简化的研究/知识图标 -->
                <g transform="translate(50, 50)">
                    <!-- 书本图标 -->
                    <path d="M -25 -15 L 25 -15 L 25 15 L -25 15 Z" fill="#2563eb" opacity="0.2"/>
                    <path d="M -20 -10 L 20 -10 L 20 10 L -20 10 Z" fill="#2563eb"/>
                    <!-- 知识之光 -->
                    <circle cx="0" cy="-20" r="8" fill="#ffffff"/>
                    <path d="M 0 -12 L -5 -5 L 5 -5 Z" fill="#ffffff"/>
                </g>
                <!-- 文字 -->
                <text x="50" y="75" font-family="Arial, sans-serif" font-size="14" font-weight="bold" fill="#2563eb" text-anchor="middle">智研</text>
            </svg>
            """;
    }

    /**
     * 构建邮件内容（保留作为备用）
     */
    private String buildEmailContent(String code, VerificationCodeType type) {
        String action;
        switch (type) {
            case REGISTER:
                action = "注册账户";
                break;
            case RESET_PASSWORD:
                action = "重置密码";
                break;
            case CHANGE_EMAIL:
                action = "变更邮箱";
                break;
            default:
                action = "身份验证";
                break;
        }

        return String.format(
                """
                您好！
                这里是%s,
                您正在进行%s操作,验证码为：%s
                
                验证码有效期为%d分钟,请及时使用。
                如果这不是您的操作,请忽略此邮件。
                请不要回复该邮件,
                感谢您的使用,
                
                %s团队""",
                appName, action, code, CODE_EXPIRE_MINUTES, appName
        );
    }


    /**
     * 读取 zylogo.png 并转换为 Base64 Data URI
     * 要求：zylogo.png 必须位于 classpath 下的 /logo/ 目录中
     */
    private String getLogoPngBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("logo/zylogo.png");
            if (!resource.exists()) {
                throw new IOException("Logo file not found in classpath: logo/zylogo.png");
            }
            byte[] imageBytes = resource.getInputStream().readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/png;base64," + base64Image;
        } catch (IOException e) {
            log.warn("Failed to load PNG logo, falling back to simplified SVG", e);
            // Fallback to SVG if PNG is missing
            String simplifiedSvg = buildSimplifiedLogoSvg();
            String base64Svg = Base64.getEncoder().encodeToString(simplifiedSvg.getBytes(StandardCharsets.UTF_8));
            return "data:image/svg+xml;base64," + base64Svg;
        }
    }
}