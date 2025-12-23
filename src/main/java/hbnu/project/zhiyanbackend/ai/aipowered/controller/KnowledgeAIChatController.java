package hbnu.project.zhiyanbackend.ai.aipowered.controller;

import hbnu.project.zhiyanbackend.ai.aiassistant.service.DifyFileService;
import hbnu.project.zhiyanbackend.ai.aipowered.config.KnowledgeDifyProperties;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.sse.core.DifyStreamEmitter;
import hbnu.project.zhiyanbackend.sse.service.DifyStreamService;
import hbnu.project.zhiyanbackend.basic.domain.R;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;
import static io.micrometer.common.util.StringUtils.truncate;

/**
 * 知识库AI对话控制器
 * 提供与知识库相关的AI对话功能，支持流式响应和文件上传
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/ai/dify/knowledge")
@RequiredArgsConstructor
public class KnowledgeAIChatController {

    private final DifyFileService difyFileService;
    private final KnowledgeDifyProperties knowledgeDifyProperties;
    private final DifyStreamService difyStreamService;

    /**
     * 上传单个本地文件到 Dify
     * 同步上传并立即返回 Dify 文件ID，前端可直接使用
     *
     * @param file 要上传的文件
     * @return 上传结果，包含 Dify 文件ID
     */
    @PostMapping("/files/upload")
    public R<DifyFileUploadResponse> uploadLocalFile(@RequestParam("file") MultipartFile file) {
        Long userId = validateAndGetUserId();

        log.info("[Knowledge Dify 异步上传] 创建上传任务, fileName={}, size={} bytes, userId={}", file.getOriginalFilename(), file.getSize(), userId);

        try {
            DifyFileUploadResponse response = difyFileService.uploadFile(file, userId);
            log.info("[Knowledge Dify 文件上传] 上传成功, fileName={}, difyFileId={}", file.getOriginalFilename(), response.getFileId());
            return R.ok(response, "文件上传成功");
        } catch (Exception e) {
            log.error("[Knowledge Dify 文件上传] 上传失败, fileName={}, userId={}",
                    file.getOriginalFilename(), userId, e);
            return R.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传本地文件到 Dify
     * 同步上传并立即返回所有 Dify 文件ID
     *
     * @param files 要上传的文件列表
     * @return 上传结果列表，包含每个文件的 Dify 文件ID
     */
    @PostMapping("/files/upload/batch")
    public R<List<DifyFileUploadResponse>> uploadLocalFiles(@RequestParam("files") List<MultipartFile> files) {
        Long userId = validateAndGetUserId();

        if (files == null || files.isEmpty()) {
            return R.fail("文件列表为空");
        }

        log.info("[Knowledge Dify 批量上传] 开始上传, count={}, userId={}", files.size(), userId);

        try {
            List<DifyFileUploadResponse> responses = difyFileService.uploadFiles(files, userId);
            long successCount = responses.stream()
                    .filter(r -> r != null && r.getFileId() != null)
                    .count();

            log.info("[Knowledge Dify 批量上传] 完成 - 总数={}, 成功={}", files.size(), successCount);
            return R.ok(responses, String.format("批量上传完成，成功 %d/%d", successCount, files.size()));
        } catch (Exception e) {
            log.error("[Knowledge Dify 批量上传] 失败 - count={}, userId={}", files.size(), userId, e);
            return R.fail("批量上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传知识库文件到 Dify，预上传模式
     * 用户在对话前预先上传知识库文件，获取 difyFileId 后再开始对话
     *
     * @param request 包含知识库文件ID列表的请求体 {"knowledgeFileIds": [1, 2, 3]}
     * @return 上传结果详情，包含每个文件的 Dify 文件ID
     */
    @PostMapping("/files/upload/knowledge")
    public R<Map<String, Object>> uploadKnowledgeFiles(@RequestBody Map<String, Object> request) {
        Long userId = validateAndGetUserId();

        List<Long> knowledgeFileIds = new ArrayList<>();
        Object fileObject = request.get("knowledgeFileIds");

        if(fileObject instanceof List<?> list){
            for(Object object : list){
                try{
                    if(object instanceof Number){
                        knowledgeFileIds.add(((Number) object).longValue());
                    } else if (object instanceof String) {
                        // 前端可能传递字符串格式的ID
                        knowledgeFileIds.add(Long.parseLong((String) object));
                    }
                }catch (NumberFormatException e) {
                    log.warn("[Knowledge Dify] 无法解析文件ID: {}", object);
                }
            }
        }

        if (knowledgeFileIds.isEmpty()) {
            return R.fail("文件ID列表为空或格式错误");
        }

        log.info("[Knowledge Dify 知识库上传] 开始上传知识库文件, userId={}, fileIds={}", userId, knowledgeFileIds);

        try {
            List<DifyFileUploadResponse> uploadResponses = difyFileService.uploadKnowledgeFiles(knowledgeFileIds, userId);

            // 构建详细的返回结果
            List<Map<String, Object>> results = buildUploadResults(knowledgeFileIds, uploadResponses);

            long successCount = results.stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("success")))
                    .count();

            log.info("[Knowledge Dify 知识库上传] 完成 - 总数={}, 成功={}", knowledgeFileIds.size(), successCount);

            Map<String, Object> data = new HashMap<>();
            data.put("total", knowledgeFileIds.size());
            data.put("success", successCount);
            data.put("failed", knowledgeFileIds.size() - successCount);
            data.put("results", results);

            return R.ok(data, String.format("知识库文件上传完成，成功 %d/%d", successCount, knowledgeFileIds.size()));
        } catch (Exception e) {
            log.error("[Knowledge Dify 知识库上传] 失败 - fileIds={}, userId={}", knowledgeFileIds, userId, e);
            return R.fail("知识库文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 通用流式AI对话（支持带文件 - 使用已上传的 Dify 文件ID，和不带文件）
     * 流程：
     * 1. 用户选择文件
     * 2. 调用 /files/upload/knowledge 或 /files/upload/batch 上传文件
     * 3. 获得 difyFileIds（例如：["file-xxx", "file-yyy"]）
     * 4. 调用此接口，传入 query、conversationId 和 difyFileIds
     * 5. Dify 在响应中返回 conversation_id（在 message 事件中）
     * 6. 前端保存 conversation_id 用于后续对话
     *
     * @param query 用户询问内容
     * @param conversationId 返回的会话ID，首次传递空
     * @param difyFileIds 已上传到Dify的文件ID列表（预上传方式）
     * @return  SSE流式响应，响应中会包含 Dify 返回的 conversation_id
     */
    @PostMapping(value = "/chat/stream-with-files", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamWithFiles(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "") String conversationId,
            @RequestParam(required = false, name = "difyFileIds") List<String> difyFileIds) {

        Long userId = validateAndGetUserId();

        // 使用临时ID用于内部管理SSE连接
        String internalEmitterId = UUID.randomUUID().toString();

        log.info("[Knowledge Dify 对话] 带文件的流式对话:");
        log.info("  - userId: {}", userId);
        log.info("  - difyConversationId: {}", conversationId.isEmpty() ? "新对话" : conversationId);
        log.info("  - query前50字符: {}", truncate(query, 50));
        log.info("  - difyFileIds: {}", difyFileIds);

        // 创建SSE发射器
        SseEmitter emitter = DifyStreamEmitter.createEmitter(internalEmitterId, userId);

        try {
            // 验证并过滤文件ID列表
            List<String> validFileIds = difyFileIds != null ?
                    difyFileIds.stream()
                            .filter(id -> id != null && !id.trim().isEmpty())
                            .collect(Collectors.toList()) :
                    Collections.emptyList();

            Map<String, Object> body;
            if (!validFileIds.isEmpty()) {
                log.info("[Knowledge Dify 对话] 检测到文件，构建带文件的请求体, count={}", validFileIds.size());
                body = buildRequestBodyWithFiles(query, conversationId, userId, validFileIds);
            } else {
                log.info("[Knowledge Dify 对话] 未检测到文件，构建基础请求体");
                body = buildBasicRequestBody(query, conversationId, userId);
            }

            log.info("[Knowledge Dify 对话] 请求体构建完成:");
            log.info("  - conversationId: {}", conversationId.isEmpty() ? "新对话" : conversationId);
            log.info("  - filesCount: {}", validFileIds.size());
            log.info("  - files详情: {}", body.get("inputs"));

            // 调用Dify流式服务
            callDifyStreamWithLogging(internalEmitterId, body);

        } catch (Exception e) {
            log.error("[Knowledge Dify 对话] 处理请求失败", e);
            DifyStreamEmitter.sendError(internalEmitterId, "处理请求失败: " + e.getMessage());
        }

        return emitter;
    }

    /**
     * 停止流式响应
     *
     * @param conversationId 对话 ID
     * @return 停止结果
     */
    @PostMapping("/chat/stop/{conversationId}")
    @Operation(summary = "停止流式响应", description = "停止指定对话的流式响应")
    public R<Void> stopStream(@PathVariable String conversationId) {
        Long userId = validateAndGetUserId();
        log.info("[Knowledge Dify 停止] 请求停止响应 - conversationId={}, userId={}", conversationId, userId);

        try{
            // 验证对话是否属于当前用户
            Long conversationUserId = DifyStreamEmitter.getUserId(conversationId);
            if (conversationUserId == null || !conversationUserId.equals(userId)) {
                return R.fail("对话不存在或无权限");
            }

            // 获取 taskId
            String taskId = DifyStreamEmitter.getTaskId(conversationId);
            if (taskId == null || taskId.isEmpty()) {
                return R.fail("未找到任务ID，可能响应已完成或尚未开始");
            }

            // 调用停止接口
            boolean success = difyStreamService.stopDifyStream(
                    taskId,
                    knowledgeDifyProperties.getApiUrl(),
                    knowledgeDifyProperties.getApiKey(),
                    userId
            );

            if (success) {
                // 关闭 SSE 连接
                DifyStreamEmitter.completeEmitter(conversationId);
                return R.ok(null, "已停止响应");
            } else {
                return R.fail("停止响应失败");
            }
        }catch (ControllerException e){
            log.error("[Knowledge Dify 停止] 停止响应异常 - conversationId={}, userId={}", conversationId, userId, e);
            return R.fail("停止响应失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建基础请求体
     * 注意：即使没有文件，也要在 inputs 中设置 doc_files 为空数组，以满足 Dify API 的要求
     */
    private Map<String, Object> buildBasicRequestBody(String query, String conversationId, Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("conversation_id", conversationId != null ? conversationId : "");
        body.put("user", String.valueOf(userId));
        
        // 即使没有文件，也要设置 doc_files 为空数组，满足 Dify API 要求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("doc_files", Collections.emptyList());
        body.put("inputs", inputs);
        
        body.put("response_mode", "streaming");
        return body;
    }

    /**
     * 构建带文件的请求体
     * 注意：即使文件列表为空，也要设置 doc_files 为空数组，以满足 Dify API 的要求
     */
    private Map<String, Object> buildRequestBodyWithFiles(String query, String conversationId,
                                                          Long userId, List<String> difyFileIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("conversation_id", conversationId != null ? conversationId : "");
        body.put("user", String.valueOf(userId));

        Map<String, Object> inputs = new HashMap<>();
        List<Map<String, Object>> fileList = new ArrayList<>();
        if(difyFileIds != null && !difyFileIds.isEmpty()) {
            for(String fileId : difyFileIds) {
                // 过滤空的文件ID，避免无效参数
                if (fileId == null || fileId.trim().isEmpty()) {
                    continue;
                }
                Map<String, Object> file = new HashMap<>();
                file.put("type", "document");
                file.put("transfer_method", "local_file");
                file.put("upload_file_id", fileId.trim());
                fileList.add(file);
            }
        }
        // ⭐ Dify API 要求 doc_files 必须存在且是列表，即使没有文件也要传空数组
        inputs.put("doc_files", fileList);
        body.put("inputs", inputs);
        body.put("response_mode", "streaming");
        return body;
    }

    /**
     * 解析文件ID列表
     */
    private List<Long> parseFileIds(Object fileIdObjects) {
        if (fileIdObjects == null) {
            return Collections.emptyList();
        }

        if(!(fileIdObjects instanceof List)){
            log.warn("[Knowledge Dify] 文件ID参数类型错误, expected List, got {}", fileIdObjects.getClass().getSimpleName());
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Object> objects = (List<Object>) fileIdObjects;

        return objects.stream()
                .map(obj -> {
                    try{
                        if (obj instanceof Number) {
                            return ((Number) obj).longValue();
                        } else if (obj instanceof String) {
                            return Long.parseLong((String) obj);
                        }
                    }catch (NumberFormatException e) {
                        log.warn("[Knowledge Dify] 无法解析文件ID: {}", obj);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 验证用户登录状态并获取用户ID
     *
     * @return 用户ID
     * @throws IllegalStateException 如果用户未登录
     */
    private Long validateAndGetUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new IllegalStateException("未登录，无法进行操作");
        }
        return userId;
    }

    /**
     * 调用Dify流式服务并记录日志
     */
    private void callDifyStreamWithLogging(String internalEmitterId, Map<String, Object> body) {
        log.info("[Knowledge Dify 请求] internalEmitterId={}, apiUrl={}, apiKeyPrefix={}",
                internalEmitterId,
                knowledgeDifyProperties.getApiUrl(),
                maskApiKey(knowledgeDifyProperties.getApiKey()));

        log.debug("[Knowledge Dify 请求体] internalEmitterId={}, body={}", internalEmitterId, body);

        difyStreamService.callDifyStream(
                internalEmitterId,
                knowledgeDifyProperties.getApiUrl(),
                knowledgeDifyProperties.getApiKey(),
                body
        );
    }

    /**
     * 构建上传结果详情
     */
    private List<Map<String, Object>> buildUploadResults(List<Long> requestedFileIds,
                                                         List<DifyFileUploadResponse> uploadResponses) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < requestedFileIds.size(); i++) {
            Long fileId = requestedFileIds.get(i);
            DifyFileUploadResponse response = i < uploadResponses.size() ? uploadResponses.get(i) : null;

            Map<String, Object> result = new HashMap<>();
            result.put("knowledgeFileId", fileId);
            result.put("success", response != null && response.getFileId() != null);
            result.put("difyFileId", response != null ? response.getFileId() : null);
            result.put("fileName", response != null ? response.getFileName() : null);

            if (response == null || response.getFileId() == null) {
                result.put("error", "上传失败");
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 掩码API Key（仅显示前12个字符）
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null) {
            return "null";
        }
        int showLength = Math.min(12, apiKey.length());
        return apiKey.substring(0, showLength) + "***";
    }
}
