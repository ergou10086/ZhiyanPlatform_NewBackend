package hbnu.project.zhiyanbackend.oss.utils;

import hbnu.project.zhiyanbackend.oss.config.COSProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 腾讯云 COS 辅助工具
 *
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class COSUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final COSProperties cosProperties;

    /**
     * 根据传入目录与文件名生成对象键
     * 根据目录、自定义文件名和原始文件名，生成一个规范的 COS 对象存储键（Key）
     */
    public String buildObjectKey(String directory, String overrideFileName, String originalFilename) {
        // 针对单桶策略，优先按照业务类型映射到配置的目录
        String safeDir = resolveBusinessDirectory(directory);

        // 处理原始文件名：清理非法字符并提取文件后缀
        String normalizedOriginal = sanitizeFileName(originalFilename);
        String suffix = "";
        if (normalizedOriginal.contains(".")) {
            suffix = normalizedOriginal.substring(normalizedOriginal.lastIndexOf('.'));
        }

        // 确定目标文件名：优先使用自定义文件名，否则使用UUID
        String targetFileName = StringUtils.isNotBlank(overrideFileName)
                ? sanitizeFileName(overrideFileName)
                : UUID.randomUUID().toString().replace("-", "") + suffix;

        // 生成日期路径段并组合完整对象键
        String dateSegment = DATE_FORMATTER.format(LocalDate.now());
        return safeDir + "/" + dateSegment + "/" + targetFileName;
    }

    /**
     * 根据对象键构建公网访问 URL
     */
    public String buildPublicUrl(String objectKey) {
        // 处理域名：去除末尾斜杠
        String domain = StringUtils.stripEnd(cosProperties.getPublicDomain(), "/");
        // 对对象键中的空格进行编码
        String encodedKey = objectKey.replace(" ", "%20");
        return domain + "/" + encodedKey;
    }

    /**
     * 根据文件名和 contentType 返回有效的 contentType
     * 根据文件名推断文件的 MIME 类型（Content-Type）。
     */
    public String resolveContentType(String contentType, String filename) {
        // 提供了 contentType 就直接用
        if (StringUtils.isNotBlank(contentType)) {
            return contentType;
        }
        if (filename != null && filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename != null && (filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
            return "image/jpeg";
        }
        if (filename != null && filename.endsWith(".gif")) {
            return "image/gif";
        }
        if (filename != null && filename.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    /**
     * 生成下载用的 Content-Disposition 值
     * 确保文件下载时使用正确的文件名，支持中文文件名。
     */
    public String buildContentDisposition(String filename) {
        // 对文件名进行URL编码，替换空格编码
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        // 生成兼容各种浏览器的Content-Disposition值
        return "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded;
    }

    /**
     * 将外层传入的业务类型解析为真实目录，确保单桶多业务安全隔离。
     */
    private String resolveBusinessDirectory(String businessType) {
        if (cosProperties.getBusinessType() == null) {
            return sanitizeDirectory(businessType);
        }

        String defaultDirectory = cosProperties.getDefaultDirectory();
        if (StringUtils.isBlank(businessType)) {
            return defaultDirectory;
        }

        String normalized = businessType.trim().toLowerCase();
        COSProperties.BusinessTypeConfig biz = cosProperties.getBusinessType();
        switch (normalized) {
            case "temporary":
                return biz.getTemporary();
            case "achievement":
                return biz.getAchievement();
            case "wiki":
                return biz.getWiki();
            case "task":
                return biz.getTask();
            default:
                log.warn("未识别的业务类型: {}，将按自定义目录处理", businessType);
                return sanitizeDirectory(businessType);
        }
    }

    /**
     * 清理目录，避免出现 .. 或多余分隔符。
     */
    private String sanitizeDirectory(String directory) {
        String fallback = cosProperties.getDefaultDirectory();
        if (StringUtils.isBlank(directory)) {
            return fallback;
        }
        String sanitized = directory.replaceAll("\\\\", "/");
        // 防止通过 ../ 越级访问
        sanitized = sanitized.replace("..", "");
        sanitized = StringUtils.strip(sanitized, "/");
        return StringUtils.defaultIfBlank(sanitized, fallback);
    }

    /**
     * 清理文件名中的非法字符。
     */
    private String sanitizeFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "unnamed";
        }
        // 替换Windows文件系统中的九大非法字符：\ / : * ? " < > |
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
