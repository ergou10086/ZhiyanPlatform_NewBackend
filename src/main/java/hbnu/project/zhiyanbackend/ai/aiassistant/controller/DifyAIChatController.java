package hbnu.project.zhiyanbackend.ai.aiassistant.controller;

import hbnu.project.zhiyanbackend.ai.aiassistant.config.DifyProperties;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.DifyFileService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.sse.core.DifyStreamEmitter;
import hbnu.project.zhiyanbackend.sse.service.DifyStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/zhiyan/ai/dify")
@RequiredArgsConstructor
public class DifyAIChatController {

    private final DifyFileService difyFileService;
    private final DifyStreamService difyStreamService;
    private final DifyProperties difyProperties;

    @PostMapping("/files/upload")
    public R<DifyFileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法上传文件");
        }
        log.info("[Dify 文件上传] fileName={}, size={}, userId={}", file.getOriginalFilename(), file.getSize(), userId);
        DifyFileUploadResponse response = difyFileService.uploadFile(file, userId);
        return R.ok(response, "文件上传成功");
    }

    @PostMapping("/files/upload/batch")
    public R<List<DifyFileUploadResponse>> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法上传文件");
        }
        log.info("[Dify 批量上传] count={}, userId={}", files.size(), userId);
        List<DifyFileUploadResponse> responses = difyFileService.uploadFiles(files, userId);
        return R.ok(responses, "批量上传完成");
    }

    @PostMapping("/files/upload/knowledge")
    public R<List<DifyFileUploadResponse>> uploadKnowledgeFiles(@RequestBody List<Long> fileIds) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录，无法上传文件");
        }
        log.info("[Dify 知识库文件上传] fileIds={}, userId={}", fileIds, userId);
        List<DifyFileUploadResponse> responses = difyFileService.uploadKnowledgeFiles(fileIds, userId);
        return R.ok(responses, "知识库文件上传完成");
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String query,
                                 @RequestParam(required = false) String conversationId,
                                 @RequestParam(required = false, name = "difyFileIds") List<String> difyFileIds,
                                 @RequestParam(required = false, name = "knowledgeFileIds") List<Long> knowledgeFileIds,
                                 @RequestParam(required = false, name = "localFiles") List<MultipartFile> localFiles) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new IllegalStateException("未登录，无法进行 AI 对话");
        }

        String rawConvId = conversationId == null ? "" : conversationId.trim();
        if (!rawConvId.isEmpty() && rawConvId.contains(",")) {
            rawConvId = rawConvId.split(",")[0].trim();
        }

        String convId = rawConvId.isEmpty()
                ? UUID.randomUUID().toString()
                : rawConvId;

        SseEmitter emitter = DifyStreamEmitter.createEmitter(convId, userId);

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        // 暂时总是让 Dify 视为新会话，避免使用本地 UUID 作为 conversation_id 导致 Conversation Not Exists
        body.put("conversation_id", "");
        body.put("user", String.valueOf(userId));
        body.put("inputs", new HashMap<>());

        // 收集所有要发送给 Dify 的文件 ID
        List<String> allDifyFileIds = new ArrayList<>();
        if (difyFileIds != null && !difyFileIds.isEmpty()) {
            allDifyFileIds.addAll(difyFileIds);
        }

        if (knowledgeFileIds != null && !knowledgeFileIds.isEmpty()) {
            log.info("[Dify 对话] 附带知识库文件, count={}", knowledgeFileIds.size());
            List<DifyFileUploadResponse> knowledgeResponses = difyFileService.uploadKnowledgeFiles(knowledgeFileIds, userId);
            for (DifyFileUploadResponse resp : knowledgeResponses) {
                if (resp != null && resp.getFileId() != null) {
                    allDifyFileIds.add(resp.getFileId());
                }
            }
        }

        // 处理本地上传文件（通过 multipart/form-data 传入）
        if (localFiles != null && !localFiles.isEmpty()) {
            log.info("[Dify 对话] 附带本地上传文件, count={}", localFiles.size());
            List<DifyFileUploadResponse> uploadResponses = difyFileService.uploadFiles(localFiles, userId);
            for (DifyFileUploadResponse resp : uploadResponses) {
                if (resp != null && resp.getFileId() != null) {
                    allDifyFileIds.add(resp.getFileId());
                }
            }
        }
        log.info("[Dify Config] apiUrl={}, apiKeyPrefix={}",
         difyProperties.getApiUrl(),
         difyProperties.getApiKey() != null
             ? difyProperties.getApiKey().substring(0, 12)
             : "null");

        if (!allDifyFileIds.isEmpty()) {
            List<Map<String, Object>> files = new ArrayList<>();
            for (String fileId : allDifyFileIds) {
                Map<String, Object> file = new HashMap<>();
                file.put("type", "document");
                file.put("transfer_method", "local_file");
                file.put("upload_file_id", fileId);
                files.add(file);
            }
            body.put("files", files);
        }

        difyStreamService.callDifyStream(convId, difyProperties.getApiUrl(), difyProperties.getApiKey(), body);

        return emitter;
    }

    @GetMapping("/conversation/new")
    public R<String> createNewConversation() {
        String convId = UUID.randomUUID().toString();
        return R.ok(convId, "对话会话创建成功");
    }

    @GetMapping("/health")
    public R<String> health() {
        return R.ok("AI Service is running");
    }
}
