package hbnu.project.zhiyanbackend.ai.aipowered.controller;

import hbnu.project.zhiyanbackend.ai.aipowered.config.KnowledgeDifyProperties;
import hbnu.project.zhiyanbackend.ai.aipowered.service.KnowledgeDifyFileService;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.sse.core.DifyStreamEmitter;
import hbnu.project.zhiyanbackend.sse.service.DifyStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库AI对话控制器
 * 提供与知识库相关的AI对话功能，支持流式响应和文件上传
 *
 *
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/ai/dify/knowledge")
@RequiredArgsConstructor
public class KnowledgeAIChatController {

    // 注入知识库Dify文件服务
    private final KnowledgeDifyFileService knowledgeDifyFileService;
    // 注入知识库Dify配置属性
    private final KnowledgeDifyProperties knowledgeDifyProperties;
    // 注入Dify流式服务
    private final DifyStreamService difyStreamService;

    /**
     * 上传知识库文件到 Dify（提前上传）
     * @param request 包含知识库文件ID列表的请求体
     * @return 上传结果列表
     */
    @PostMapping("/upload-knowledge-files")
    public ResponseEntity<Map<String, Object>> uploadKnowledgeFiles(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new IllegalStateException("未登录，无法上传文件");
        }

        @SuppressWarnings("unchecked")
        List<Object> fileIdObjects = (List<Object>) request.get("knowledgeFileIds");
        if (fileIdObjects == null || fileIdObjects.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "code", 400,
                "msg", "文件ID列表为空",
                "data", List.of()
            ));
        }

        // 转换为 Long 类型
        List<Long> knowledgeFileIds = fileIdObjects.stream()
                .map(obj -> {
                    if (obj instanceof Number) {
                        return ((Number) obj).longValue();
                    } else if (obj instanceof String) {
                        return Long.parseLong((String) obj);
                    }
                    return null;
                })
                .filter(id -> id != null)
                .toList();

        log.info("[Knowledge Dify] 提前上传知识库文件, userId={}, fileIds={}", userId, knowledgeFileIds);

        try {
            List<DifyFileUploadResponse> uploadResponses = knowledgeDifyFileService.uploadKnowledgeFiles(knowledgeFileIds, userId);
            
            // 构建返回结果
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < knowledgeFileIds.size(); i++) {
                Long fileId = knowledgeFileIds.get(i);
                DifyFileUploadResponse response = i < uploadResponses.size() ? uploadResponses.get(i) : null;
                
                Map<String, Object> result = new HashMap<>();
                result.put("knowledgeFileId", String.valueOf(fileId)); // 转换为字符串避免精度丢失
                result.put("success", response != null && response.getFileId() != null);
                result.put("difyFileId", response != null ? response.getFileId() : null);
                result.put("fileName", response != null ? response.getFileName() : null);
                result.put("error", response == null ? "上传失败" : null);
                
                results.add(result);
            }

            log.info("[Knowledge Dify] 文件上传完成, 成功={}, 总数={}", 
                    results.stream().filter(r -> (Boolean) r.get("success")).count(), 
                    results.size());

            return ResponseEntity.ok(Map.of(
                "code", 200,
                "msg", "上传完成",
                "data", results
            ));
        } catch (Exception e) {
            log.error("[Knowledge Dify] 上传知识库文件失败", e);
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "msg", "上传失败: " + e.getMessage(),
                "data", List.of()
            ));
        }
    }

    /**
     * 处理带文件的流式AI对话请求
     * @param query 用户查询内容
     * @param conversationId 会话ID（可选）
     * @param localFiles 本地文件列表（可选）
     * @param knowledgeFileIds 知识库文件ID列表（可选）
     * @return SseEmitter 服务器发送事件发射器，用于流式响应
     */
    @PostMapping(value = "/chat/stream-with-files", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamWithFiles(@RequestParam String query,
                                          @RequestParam(required = false) String conversationId,
                                          @RequestParam(required = false, name = "localFiles") List<MultipartFile> localFiles,
                                          @RequestParam(required = false, name = "knowledgeFileIds") List<Long> knowledgeFileIds,
                                          @RequestParam(required = false, name = "difyFileIds") List<String> difyFileIds) {
        // 调试日志：打印接收到的参数
        log.info("[Knowledge Dify] 接收到请求参数:");
        log.info("  - query: {}", query);
        log.info("  - conversationId: {}", conversationId);
        log.info("  - localFiles: {}", localFiles != null ? localFiles.size() : "null");
        log.info("  - knowledgeFileIds: {}", knowledgeFileIds);
        log.info("  - knowledgeFileIds size: {}", knowledgeFileIds != null ? knowledgeFileIds.size() : "null");
        log.info("  - difyFileIds: {}", difyFileIds);
        log.info("  - difyFileIds size: {}", difyFileIds != null ? difyFileIds.size() : "null");
        
        // 获取当前用户ID，如果未登录则抛出异常
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new IllegalStateException("未登录，无法进行 AI 对话");
        }

        // 处理会话ID，如果为空则生成新的UUID
        String rawConvId = conversationId == null ? "" : conversationId.trim();
        if (!rawConvId.isEmpty() && rawConvId.contains(",")) {
            rawConvId = rawConvId.split(",")[0].trim();
        }

        String convId = rawConvId.isEmpty()
                ? UUID.randomUUID().toString()
                : rawConvId;

        // 创建流式响应发射器
        SseEmitter emitter = DifyStreamEmitter.createEmitter(convId, userId);

        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("conversation_id", "");
        body.put("user", String.valueOf(userId));

        // 构建输入参数
        Map<String, Object> inputs = new HashMap<>();
        body.put("inputs", inputs);

        // 存储所有Dify文件ID
        List<String> allDifyFileIds = new ArrayList<>();

        // 处理已上传的 Dify 文件ID（提前上传的文件）
        if (difyFileIds != null && !difyFileIds.isEmpty()) {
            log.info("[Knowledge Dify] 附带已上传的Dify文件, count={}", difyFileIds.size());
            allDifyFileIds.addAll(difyFileIds);
        }

        // 处理知识库文件（需要即时上传）
        if (knowledgeFileIds != null && !knowledgeFileIds.isEmpty()) {
            log.info("[Knowledge Dify] 附带知识库文件（需即时上传）, count={}", knowledgeFileIds.size());
            List<DifyFileUploadResponse> knowledgeResponses =
                    knowledgeDifyFileService.uploadKnowledgeFiles(knowledgeFileIds, userId);
            for (DifyFileUploadResponse resp : knowledgeResponses) {
                if (resp != null && resp.getFileId() != null) {
                    allDifyFileIds.add(resp.getFileId());
                }
            }
        }

        // 处理本地上传文件
        if (localFiles != null && !localFiles.isEmpty()) {
            log.info("[Knowledge Dify] 附带本地上传文件, count={}", localFiles.size());
            List<DifyFileUploadResponse> uploadResponses =
                    knowledgeDifyFileService.uploadFiles(localFiles, userId);
            for (DifyFileUploadResponse resp : uploadResponses) {
                if (resp != null && resp.getFileId() != null) {
                    allDifyFileIds.add(resp.getFileId());
                }
            }
        }

        // 构造 files 输入参数（即使没有文件也传空数组，满足 Dify 对必填项的要求）
        List<Map<String, Object>> files = new ArrayList<>();
        for (String fileId : allDifyFileIds) {
            Map<String, Object> file = new HashMap<>();
            file.put("type", "document");
            file.put("transfer_method", "local_file");
            file.put("upload_file_id", fileId);
            files.add(file);
        }
        inputs.put("files", files);
        
        log.info("[Knowledge Dify] 最终发送的文件列表: allDifyFileIds={}, files={}", allDifyFileIds, files);
        log.info("[Knowledge Dify] 完整请求体: {}", body);

        log.info("[Knowledge Dify Config] apiUrl={}, apiKeyPrefix={}",
                knowledgeDifyProperties.getApiUrl(),
                knowledgeDifyProperties.getApiKey() != null
                        ? knowledgeDifyProperties.getApiKey().substring(0, Math.min(12, knowledgeDifyProperties.getApiKey().length()))
                        : "null");

        difyStreamService.callDifyStream(convId,
                knowledgeDifyProperties.getApiUrl(),
                knowledgeDifyProperties.getApiKey(),
                body);

        return emitter;
    }
}
