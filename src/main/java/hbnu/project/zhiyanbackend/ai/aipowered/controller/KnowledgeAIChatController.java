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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
                                          @RequestParam(required = false, name = "knowledgeFileIds") List<Long> knowledgeFileIds) {
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

        // 处理知识库文件
        if (knowledgeFileIds != null && !knowledgeFileIds.isEmpty()) {
            log.info("[Knowledge Dify] 附带知识库文件, count={}", knowledgeFileIds.size());
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
