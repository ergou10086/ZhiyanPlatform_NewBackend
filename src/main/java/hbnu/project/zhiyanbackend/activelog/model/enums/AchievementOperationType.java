package hbnu.project.zhiyanbackend.activelog.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 成果操作类型枚举
 *
 * @author ErgouTree
 */
@Getter
@AllArgsConstructor
public enum AchievementOperationType {

    /**
     * 创建成果
     */
    CREATE("CREATE", "创建成果"),

    /**
     * 更新成果
     */
    UPDATE("UPDATE", "更新成果"),

    /**
     * 删除成果
     */
    DELETE("DELETE", "删除成果"),

    /**
     * 文件上传
     */
    FILE_UPLOAD("FILE_UPLOAD", "文件上传"),

    /**
     * 文件删除
     */
    FILE_DELETE("FILE_DELETE", "文件删除");

    private final String code;
    private final String desc;
}
