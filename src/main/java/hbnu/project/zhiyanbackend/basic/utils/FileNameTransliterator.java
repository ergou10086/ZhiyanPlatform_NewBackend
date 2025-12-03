package hbnu.project.zhiyanbackend.basic.utils;

import cn.hutool.extra.pinyin.PinyinUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 将包含中文或特殊字符的文件名转换为 COS 可接受的 ASCII 文件名。
 * 使用 Hutool 的拼音转换并结合简单的 slug 规则，确保最终只包含
 * 字母、数字、连字符和下划线，同时保留原始扩展名。
 */
public final class FileNameTransliterator {

    private FileNameTransliterator() {
    }

    /**
     * 将任意文件名转换为 ASCII 文件名。
     *
     * @param originalFilename 原始文件名
     * @return 转换后的 ASCII 文件名，保留扩展名
     */
    public static String toAsciiFileName(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            return defaultName();
        }

        String trimmed = originalFilename.trim();
        String extension = "";
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot >= 0) {
            extension = trimmed.substring(lastDot + 1);
            trimmed = trimmed.substring(0, lastDot);
        }

        String asciiBase = transliterate(trimmed);
        if (StringUtils.isBlank(asciiBase)) {
            asciiBase = defaultName();
        }

        String asciiExt = transliterate(extension);
        String extensionPart = StringUtils.isNotBlank(asciiExt) ? "." + asciiExt : "";
        return asciiBase + extensionPart;
    }

    private static String transliterate(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }

        // 使用 Hutool 将中文转换为拼音（无声调，全部小写）
        String pinyin = PinyinUtil.getPinyin(text, "");

        if (StringUtils.isBlank(pinyin)) {
            pinyin = text;
        }

        // 归一化并剔除非 ASCII 字符
        String normalized = Normalizer.normalize(pinyin, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        normalized = normalized
                .replaceAll("[^a-zA-Z0-9_-]+", "-")
                .replaceAll("[-_]{2,}", "-");

        normalized = StringUtils.strip(normalized, "-_");

        return StringUtils.isNotBlank(normalized)
                ? normalized.toLowerCase(Locale.ROOT)
                : "";
    }

    private static String defaultName() {
        return "file-" + System.currentTimeMillis();
    }
}


