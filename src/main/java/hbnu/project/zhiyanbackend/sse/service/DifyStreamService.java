package hbnu.project.zhiyanbackend.sse.service;


import hbnu.project.zhiyanbackend.sse.core.DifyStreamEmitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Dify 流式调用服务
 * 处理与 Dify API 的流式交互
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DifyStreamService {

    private final WebClient.Builder webClientBuilder;


    /**
     * 调用 Dify API 并通过 SSE 流式返回结果
     *
     * @param conversationId 对话 ID
     * @param apiUrl Dify API 地址
     * @param apiKey API Key
     * @param requestBody 请求体
     */
    public void callDifyStream(
            String conversationId,
            String apiUrl,
            String apiKey,
            Map<String, Object> requestBody
    ){

        log.info("[Dify Stream] 开始流式调用: conversationId={}", conversationId);

        WebClient webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                // 明确声明期望 SSE 文本流
                .defaultHeader("Accept", "text/event-stream")
                .build();

        // 流式模式，由 Dify 以 SSE 方式返回 data: {...}
        requestBody.put("response_mode", "streaming");

        webClient.post()
                .uri("/chat-messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    // 逐块解析 Dify 的 SSE 响应
                    handleDifyChunk(conversationId, chunk);
                })
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        String responseBody = ex.getResponseBodyAsString();
                        log.error("[Dify Stream] 调用错误: conversationId={}, status={}, body={}",
                                conversationId,
                                ex.getStatusCode(),
                                responseBody,
                                ex);
                        DifyStreamEmitter.sendError(conversationId,
                                "AI 调用失败: " + ex.getStatusCode() + " - " + responseBody);
                    } else {
                        log.error("[Dify Stream] 调用错误: conversationId={}", conversationId, error);
                        DifyStreamEmitter.sendError(conversationId, "AI 调用失败: " + error.getMessage());
                    }
                })
                .doOnComplete(() -> {
                    log.info("[Dify Stream] 调用完成: conversationId={}", conversationId);
                    DifyStreamEmitter.completeEmitter(conversationId);
                })
                .subscribe();

    }



    /**
     * 处理 Dify 返回的数据块
     */
    private void handleDifyChunk(String conversationId, String chunk) {
        try {
            // Dify 返回的是 SSE 格式：data: {...}，在 WebClient 中通常已经解码为 data 内容本身
            if (chunk == null || chunk.isEmpty()) {
                return;
            }

            String jsonData = chunk.trim();

            // 兼容带有 "data:" 前缀的情况
            if (jsonData.startsWith("data:")) {
                jsonData = jsonData.substring(5).trim();
            }

            // 跳过心跳包
            if ("[DONE]".equals(jsonData)) {
                return;
            }

            // 解析 JSON 并提取文本
            // TODO:这里简化处理，实际需要根据 Dify 返回格式解析
            String text = extractTextFromDifyResponse(jsonData);

            if (text != null && !text.isEmpty()) {
                DifyStreamEmitter.sendChunk(conversationId, text);
            }
        } catch (Exception e) {
            log.error("[Dify Stream] 处理数据块失败: conversationId={}", conversationId, e);
        }
    }



    /**
     * 从 Dify 响应中提取文本
     * 需要根据实际 Dify API 响应格式调整
     */
    private String extractTextFromDifyResponse(String jsonData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonData);

            // 优先从 data 字段中取 answer（Dify 标准结构）
            JsonNode dataNode = root.path("data");
            if (dataNode != null && !dataNode.isMissingNode()) {
                // data 直接是文本
                if (dataNode.isTextual()) {
                    return dataNode.asText();
                }

                // data 是对象，优先取 answer，其次 output_text
                if (dataNode.isObject()) {
                    if (dataNode.has("answer")) {
                        return dataNode.get("answer").asText("");
                    }
                    if (dataNode.has("output_text")) {
                        return dataNode.get("output_text").asText("");
                    }
                }
            }

            // 兼容顶层直接包含 answer 的情况
            if (root.has("answer")) {
                return root.get("answer").asText("");
            }

            // 未找到可用文本时返回空，避免前端显示整段 JSON
            return "";
        } catch (Exception e) {
            log.error("[Dify Stream] 解析 JSON 响应失败, 返回原始数据", e);
            return jsonData;
        }
    }
}
