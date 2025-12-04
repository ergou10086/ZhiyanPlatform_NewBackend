package hbnu.project.zhiyanbackend.knowledge.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 成果状态枚举
 *
 * @author ErgouTree
 */
@Getter
public enum AchievementStatus {

    /**
     * 草稿
     */
    draft("draft", "草稿"),

    /**
     * 审核中
     */
    under_review("under_review", "审核中"),

    /**
     * 已发布
     */
    published("published", "已发布"),

    /**
     * 已过时
     */
    obsolete("obsolete", "已过时");

    /**
     * 英文标识（兼容原有逻辑，保持接口一致性）
     */
    private final String value;

    /**
     * 中文名称（用于前端显示）
     */
    private final String chineseName;

    AchievementStatus(String value, String chineseName) {
        this.value = value;
        this.chineseName = chineseName;
    }

    /**
     * 根据英文标识获取枚举（兼容原有逻辑）
     *
     * @param value 英文标识（如 "draft"）
     * @return 对应的枚举，不存在返回 null
     */
    public static AchievementStatus getByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        for (AchievementStatus status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据中文名称获取枚举（用于前端模糊查询场景）
     *
     * @param chineseName 中文名称（如 "草稿"）
     * @return 对应的枚举，不存在返回 null
     */
    public static AchievementStatus getByChineseName(String chineseName) {
        if (StringUtils.isBlank(chineseName)) {
            return null;
        }
        for (AchievementStatus status : values()) {
            if (status.getChineseName().equals(chineseName)) {
                return status;
            }
        }
        return null;
    }
}
