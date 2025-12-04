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
        if (scene == null) {
            return LOW;
        }
        
        switch (scene) {
            case TASK_OVERDUE:
            case TASK_REVIEW_REQUEST:
            case TASK_REVIEW_RESULT:
            case PROJECT_MEMBER_APPLY:
            case SYSTEM_SECURITY_ALERT:
            case PROJECT_MEMBER_INVITED:
                return HIGH;
            case TASK_DEADLINE_REMIND:
            case PROJECT_ROLE_CHANGED:
            case ACHIEVEMENT_REVIEW_REQUEST:
            case ACHIEVEMENT_STATUS_CHANGED:
            case WIKI_PAGE_UPDATED:
            case WIKI_PAGE_DELETED:
                return MEDIUM;
            case WIKI_PAGE_CREATED:
            case USER_CUSTOM_MESSAGE:
            default:
                return LOW;
        }
    }
}
