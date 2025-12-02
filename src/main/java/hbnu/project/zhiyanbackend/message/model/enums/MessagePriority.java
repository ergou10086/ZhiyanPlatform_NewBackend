package hbnu.project.zhiyanbackend.message.model.enums;

/**
 * 任务优先级枚举
 *
 * @author ErgouTree
 */
public enum MessagePriority {

    HIGH,
    MEDIUM,
    LOW;

    public static MessagePriority ofScene(MessageScene scene) {
        return switch (scene) {
            case TASK_OVERDUE, TASK_REVIEW_REQUEST, TASK_REVIEW_RESULT,
                 PROJECT_MEMBER_APPLY, SYSTEM_SECURITY_ALERT -> HIGH;
            case TASK_DEADLINE_REMIND, PROJECT_ROLE_CHANGED,
                 ACHIEVEMENT_REVIEW_REQUEST, ACHIEVEMENT_STATUS_CHANGED,
                 WIKI_PAGE_UPDATED, WIKI_PAGE_DELETED -> MEDIUM;
            case WIKI_PAGE_CREATED, USER_CUSTOM_MESSAGE -> LOW;
            default -> LOW;
        };
    }
}
