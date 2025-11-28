package hbnu.project.zhiyanbackend.tasks.model.enums;

/**
 * 任务分配类型
 *
 * @author Tokito
 */
public enum AssignType {

    /**
     * 被管理员/他人分配
     */
    ASSIGNED("ASSIGNED", "被管理员或负责人分配"),

    /**
     * 用户主动接取
     */
    CLAIMED("CLAIMED", "用户主动接取");

    private final String code;
    private final String description;

    AssignType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
