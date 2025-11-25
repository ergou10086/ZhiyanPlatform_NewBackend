package hbnu.project.zhiyanbackend.security.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 密码工具类
 * 提供密码强度验证、密码生成等功能
 *
 * @author ErgouTree
 */
public class PasswordUtils {

    /**
     * 密码强度：弱（只包含一种字符类型）
     */
    private static final Pattern WEAK_PASSWORD = Pattern.compile(
            "^[0-9]+$|^[a-zA-Z]+$|^[^a-zA-Z0-9]+$"
    );

    /**
     * 密码强度：中等（包含两种字符类型）
     */
    private static final Pattern MEDIUM_PASSWORD = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-zA-Z])[^!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]*$|" +
                    "^(?=.*[0-9])(?=.*[^a-zA-Z0-9])[a-zA-Z!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]*$|" +
                    "^(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9])[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]*$"
    );

    /**
     * 密码强度：强（包含三种字符类型）
     */
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).+$"
    );

    /**
     * 验证密码强度
     *
     * @param password 密码
     * @return 密码强度等级（1-弱，2-中等，3-强）
     */
    public static int validatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            return 0; // 无效密码
        }

        if (STRONG_PASSWORD.matcher(password).matches()) {
            return 3; // 强密码
        } else if (MEDIUM_PASSWORD.matcher(password).matches()) {
            return 2; // 中等密码
        } else if (WEAK_PASSWORD.matcher(password).matches()) {
            return 1; // 弱密码
        } else {
            return 0; // 无效密码
        }
    }

    /**
     * 检查密码是否符合基本要求
     *
     * @param password 密码
     * @return 是否符合要求
     */
    public static boolean isValidPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return false;
        }

        // 长度检查：6-16位
        if (password.length() < 6 || password.length() > 16) {
            return false;
        }

        // 允许任何可见字符，排除控制字符
        return password.matches("^[\\x20-\\x7E]+$");
    }

    /**
     * 使用SecurityUtils加密密码
     * 这是一个便捷方法，实际调用SecurityUtils.encryptPassword
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String password) {
        return SecurityUtils.encryptPassword(password);
    }

    /**
     * 使用SecurityUtils验证密码
     * 这是一个便捷方法，实际调用SecurityUtils.matchesPassword
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密密码
     * @return 是否匹配
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        return SecurityUtils.matchesPassword(rawPassword, encodedPassword);
    }

    /**
     * 获取密码强度描述
     *
     * @param password 密码
     * @return 密码强度描述
     */
    public static String getPasswordStrengthDescription(String password) {
        int strength = validatePasswordStrength(password);
        return switch (strength) {
            case 0 -> "密码无效";
            case 1 -> "密码强度：弱";
            case 2 -> "密码强度：中等";
            case 3 -> "密码强度：强";
            default -> "未知";
        };
    }

    /**
     * 检查密码是否包含空格
     *
     * @param password 密码
     * @return 是否包含空格
     */
    public static boolean containsWhitespace(String password) {
        return password != null && password.contains(" ");
    }

    /**
     * 获取密码包含的字符类型数量
     *
     * @param password 密码
     * @return 字符类型数量（数字、字母、特殊字符）
     */
    public static int getCharacterTypeCount(String password) {
        if (password == null) {
            return 0;
        }

        int typeCount = 0;

        // 检查是否包含数字
        if (password.matches(".*[0-9].*")) {
            typeCount++;
        }

        // 检查是否包含字母
        if (password.matches(".*[a-zA-Z].*")) {
            typeCount++;
        }

        // 检查是否包含特殊字符
        if (password.matches(".*[^a-zA-Z0-9].*")) {
            typeCount++;
        }

        return typeCount;
    }
}