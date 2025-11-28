package hbnu.project.zhiyanbackend.tasks.model.enums;

/**
 * 任务内用户角色类型
 *
 * @author Tokito
 */
public enum RoleType {

    /**
     * 执行者
     */
    EXECUTOR("EXECUTOR", "任务执行者"),

    /**
     * 关注者
     */
    FOLLOWER("FOLLOWER", "任务关注者"),

    /**
     * 审核人
     */
    REVIEWER("REVIEWER", "任务审核人");

    private final String code;
    private final String description;

    RoleType(String code, String description) {
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
