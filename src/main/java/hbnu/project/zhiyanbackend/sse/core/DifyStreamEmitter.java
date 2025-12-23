package hbnu.project.zhiyanbackend.sse.core;

import hbnu.project.zhiyanbackend.basic.exception.SseException;
import hbnu.project.zhiyanbackend.sse.dto.DifyStreamMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dify 流式响应发射器管理器
 * 专门用于管理 AI 对话的流式响应
 *
 * @author ErgouTree
 */
@Slf4j
public class DifyStreamEmitter {

    /**
     * 存储每个会话的 SSE 发射器
     * Key: conversationId, Value: SseEmitter
     */
    private static final Map<String, SseEmitter> CONVERSATION_EMITTERS = new ConcurrentHashMap<>();

    /**
     * SSE 超时时间：10分钟（AI 对话可能较长）
     */
    private static final Long SSE_TIMEOUT = 600000L;

    /**
     * 存储每个会话的 userId（用于停止响应时验证）
     * Key: conversationId, Value: userId
     */
    private static final Map<String, Long> CONVERSATION_USER_IDS = new ConcurrentHashMap<>();

    /**
     * 存储每个会话的 taskId（用于停止响应）
     * Key: conversationId, Value: taskId
     */
    private static final Map<String, String> CONVERSATION_TASK_IDS = new ConcurrentHashMap<>();

    /**
     * 创建新的对话流式发射器
     *
     * @param conversationId 对话 ID
     * @param userId 用户 ID
     * @return SseEmitter 实例
     */
    public static SseEmitter createEmitter(String conversationId, Long userId) {
        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT);

        // 注册回调
        sseEmitter.onCompletion(() -> {
            log.info("[Dify SSE] 对话完成: conversationId={}, userId={}", conversationId, userId);
            CONVERSATION_EMITTERS.remove(conversationId);
            CONVERSATION_TASK_IDS.remove(conversationId);
            CONVERSATION_USER_IDS.remove(conversationId);
        });

        sseEmitter.onTimeout(() -> {
            log.warn("[Dify SSE] 对话超时: conversationId={}, userId={}", conversationId, userId);
            CONVERSATION_EMITTERS.remove(conversationId);
            CONVERSATION_TASK_IDS.remove(conversationId);
            CONVERSATION_USER_IDS.remove(conversationId);
        });

        sseEmitter.onError((e) -> {
            log.error("[Dify SSE] 对话错误: conversationId={}, userId={}", conversationId, userId, e);
            CONVERSATION_EMITTERS.remove(conversationId);
            CONVERSATION_TASK_IDS.remove(conversationId);
            CONVERSATION_USER_IDS.remove(conversationId);
        });

        // 存储发射器
        CONVERSATION_EMITTERS.put(conversationId, sseEmitter);
        CONVERSATION_USER_IDS.put(conversationId, userId);

        // 发送连接成功消息，包含 conversationId（用于停止请求）
        try {
            Map<String, Object> connectedData = new HashMap<>();
            connectedData.put("message", "连接成功");
            connectedData.put("conversationId", conversationId); // 返回 internalEmitterId 给前端
            
            sseEmitter.send(SseEmitter.event()
                    .name("connected")
                    .data(connectedData));
        }catch (SseException | IOException e){
            log.error("[Dify SSE] 发送连接消息失败", e);
            CONVERSATION_EMITTERS.remove(conversationId);
            CONVERSATION_TASK_IDS.remove(conversationId);
            CONVERSATION_USER_IDS.remove(conversationId);
        }

        log.info("[Dify SSE] 创建流式发射器: conversationId={}, userId={}", conversationId, userId);
        return sseEmitter;
    }


    /**
     * 发送流式消息
     *
     * @param conversationId 对话 ID
     * @param message 消息内容
     * @param eventType 事件类型 (message/answer/error/done)
     */
    public static void sendMessage(String conversationId, String message, String eventType) {
        SseEmitter emitter = CONVERSATION_EMITTERS.get(conversationId);

        if(emitter == null){
            log.warn("[Dify SSE] 发射器不存在: conversationId={}", conversationId);
            return;
        }

        try {
            DifyStreamMessage streamMessage = DifyStreamMessage.builder()
                    .conversationId(conversationId)
                    .event(eventType)
                    .data(message)
                    .timestamp(System.currentTimeMillis())
                    .build();

            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(streamMessage));

        } catch (IOException e) {
            log.error("[Dify SSE] 发送消息失败: conversationId={}", conversationId, e);
            completeEmitter(conversationId);
        }
    }


    /**
     * 发送文本块（用于流式输出）
     *
     * @param conversationId 对话 ID
     * @param textChunk 文本块
     */
    public static void sendChunk(String conversationId, String textChunk) {
        sendMessage(conversationId, textChunk, "message");
    }


    /**
     * 发送完整回答
     *
     * @param conversationId 对话 ID
     * @param fullAnswer 完整回答
     */
    public static void sendAnswer(String conversationId, String fullAnswer) {
        sendMessage(conversationId, fullAnswer, "answer");
    }


    /**
     * 发送错误消息
     *
     * @param conversationId 对话 ID
     * @param errorMessage 错误消息
     */
    public static void sendError(String conversationId, String errorMessage) {
        sendMessage(conversationId, errorMessage, "error");
        completeEmitter(conversationId);
    }


    /**
     * 完成并关闭发射器
     *
     * @param conversationId 对话 ID
     */
    public static void completeEmitter(String conversationId) {
        SseEmitter emitter = CONVERSATION_EMITTERS.remove(conversationId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("对话完成"));
                emitter.complete();
                log.info("[Dify SSE] 关闭发射器: conversationId={}", conversationId);
            } catch (Exception e) {
                log.error("[Dify SSE] 关闭发射器失败: conversationId={}", conversationId, e);
            }
        }
    }

    /**
     * 保存 taskId
     */
    public static void saveTaskId(String conversationId, String taskId) {
        if (conversationId != null && taskId != null) {
            CONVERSATION_TASK_IDS.put(conversationId, taskId);
            log.debug("[Dify SSE] 保存 taskId: conversationId={}, taskId={}", conversationId, taskId);
        }
    }

    /**
     * 获取 taskId
     */
    public static String getTaskId(String conversationId) {
        return CONVERSATION_TASK_IDS.get(conversationId);
    }

    /**
     * 获取 userId
     */
    public static Long getUserId(String conversationId) {
        return CONVERSATION_USER_IDS.get(conversationId);
    }

    /**
     * 检查发射器是否存在
     *
     * @param conversationId 对话 ID
     * @return 是否存在
     */
    public static boolean exists(String conversationId) {
        return CONVERSATION_EMITTERS.containsKey(conversationId);
    }


    /**
     * 获取当前活跃的对话数量
     *
     * @return 活跃对话数
     */
    public static int getActiveCount() {
        return CONVERSATION_EMITTERS.size();
    }
}
