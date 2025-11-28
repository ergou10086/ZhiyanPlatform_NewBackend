package hbnu.project.zhiyanbackend.tasks.model.enums;

/**
 * 任务提交审核状态
 *
 * @author Tokito
 */
public enum ReviewStatus {

    /**
     * 待审核
     */
    PENDING("待审核"),

    /**
     * 已批准（通过）
     */
    APPROVED("已批准"),

    /**
     * 已拒绝（退回）
     */
    REJECTED("已拒绝"),

    /**
     * 已撤回（提交者主动撤回）
     */
    REVOKED("已撤回");

    private final String description;

    ReviewStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
