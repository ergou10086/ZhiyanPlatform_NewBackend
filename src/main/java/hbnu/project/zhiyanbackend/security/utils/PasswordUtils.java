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
     * 验证密码强度
     * 五级密码强度：
     * 5 - 无懈可击：>12位，有大写字母、小写字母和特殊字符
     * 4 - 高强度：≥10位，包含三种及以上字符组合（数字+字母+符号）
     * 3 - 稳健：≥8位，包含三种及以上字符组合（数字+字母+符号）
     * 2 - 入门：>7位，仅两种字符组合
     * 1 - 无效：密码强度不够平台最低标准
     * 0 - 不符合基本要求
     *
     * @param password 密码
     * @return 密码强度等级（0-5）
     */
    public static int validatePasswordStrength(String password) {
        if (password == null || password.length() < 7) {
            return 0; // 不符合基本要求
        }

        // 检查字符类型
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasSpecialChar = password.matches(".*[^a-zA-Z0-9].*");
        
        int charTypeCount = 0;
        if (hasDigit) charTypeCount++;
        if (hasLowercase || hasUppercase) charTypeCount++;
        if (hasSpecialChar) charTypeCount++;
        
        int length = password.length();
        
        // 5 - 无懈可击：>12位，有大写字母、小写字母和特殊字符
        if (length > 12 && hasLowercase && hasUppercase && hasSpecialChar) {
            return 5;
        }
        
        // 4 - 高强度：≥10位，包含三种及以上字符组合（数字+字母+符号）
        if (length >= 10 && charTypeCount >= 3) {
            return 4;
        }
        
        // 3 - 稳健：≥8位，包含三种及以上字符组合（数字+字母+符号）
        if (length >= 8 && charTypeCount >= 3) {
            return 3;
        }
        
        // 2 - 入门：>7位，仅两种字符组合
        if (length > 7 && charTypeCount == 2) {
            return 2;
        }
        
        // 1 - 无效：密码强度不够平台最低标准（虽然符合基本要求，但强度太低）
        if (length > 7 && charTypeCount == 1) {
            return 1;
        }
        
        // 0 - 不符合基本要求
        return 0;
    }

    /**
     * 检查密码是否符合基本要求
     * 新规则：
     * 1. 长度：7-25位（避免与6位2FA验证码冲突）
     * 2. 必须包含至少一个小写字母或大写字母
     * 3. 允许特殊符号（不做限制）
     *
     * @param password 密码
     * @return 是否符合要求
     */
    public static boolean isValidPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return false;
        }

        // 长度检查：7-25位（避免与6位2FA验证码冲突）
        if (password.length() < 7 || password.length() > 25) {
            return false;
        }

        // 必须包含至少一个小写字母或大写字母
        if (!password.matches(".*[a-zA-Z].*")) {
            return false;
        }

        // 允许任何可见字符，排除控制字符（允许特殊符号）
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
        return getPasswordStrengthDescriptionByLevel(strength);
    }

    /**
     * 根据强度等级获取密码强度描述
     *
     * @param strength 密码强度等级（0-5）
     * @return 密码强度描述
     */
    public static String getPasswordStrengthDescriptionByLevel(int strength) {
        return switch (strength) {
            case 0 -> "无效";
            case 1 -> "无效";
            case 2 -> "入门";
            case 3 -> "稳健";
            case 4 -> "高强度";
            case 5 -> "无懈可击";
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