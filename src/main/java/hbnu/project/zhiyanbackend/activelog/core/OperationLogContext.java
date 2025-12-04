package hbnu.project.zhiyanbackend.activelog.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 操作日志上下文
 * 用于在方法执行前设置日志信息，在切面中获取并记录
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogContext {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 业务资源ID（如 taskId/wikiPageId/achievementId）
     */
    private Long bizId;

    /**
     * 业务资源标题（如任务标题/成果标题/Wiki标题）
     */
    private String bizTitle;

    /**
     * 操作用户ID（可选，默认从SecurityContext获取）
     */
    private Long userId;

    /**
     * 操作用户名（可选，默认从SecurityContext获取）
     */
    private String username;

    /**
     * 附加信息（可选）
     */
    private String extra;

    /**
     * ThreadLocal存储当前线程的日志上下文
     */
    private static final ThreadLocal<OperationLogContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的日志上下文
     */
    public static void set(OperationLogContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前线程的日志上下文
     */
    public static OperationLogContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除当前线程的日志上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 快捷方法：设置项目ID
     */
    public static void setProjectId(Long projectId) {
        OperationLogContext context = get();
        if (context == null) {
            context = new OperationLogContext();
            set(context);
        }
        context.setProjectId(projectId);
    }

    /**
     * 快捷方法：设置业务ID
     */
    public static void setBizId(Long bizId) {
        OperationLogContext context = get();
        if (context == null) {
            context = new OperationLogContext();
            set(context);
        }
        context.setBizId(bizId);
    }

    /**
     * 快捷方法：设置业务标题
     */
    public static void setBizTitle(String bizTitle) {
        OperationLogContext context = get();
        if (context == null) {
            context = new OperationLogContext();
            set(context);
        }
        context.setBizTitle(bizTitle);
    }

    /**
     * 快捷方法：设置基本信息
     */
    public static void setBasicInfo(Long projectId, Long bizId, String bizTitle) {
        OperationLogContext context = OperationLogContext.builder()
                .projectId(projectId)
                .bizId(bizId)
                .bizTitle(bizTitle)
                .build();
        set(context);
    }
}