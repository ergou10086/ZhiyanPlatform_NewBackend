package hbnu.project.zhiyanbackend.ai.aipowered.service.impl;

import hbnu.project.zhiyanbackend.ai.aipowered.config.KnowledgeDifyProperties;
import hbnu.project.zhiyanbackend.ai.aipowered.service.KnowledgeDifyFileService;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import hbnu.project.zhiyanbackend.knowledge.model.dto.FileContextDTO;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDifyFileServiceImpl implements KnowledgeDifyFileService {

    private final KnowledgeDifyProperties knowledgeDifyProperties;
    private final RestTemplate restTemplate;
    private final AchievementFileService achievementFileService;

/**
 * 重写文件上传方法，实现将文件上传到Dify平台的功能
 * @param file 要上传的文件，MultipartFile类型
 * @param userId 用户ID，用于标识上传者
 * @return DifyFileUploadResponse 上传响应结果
 * @throws RuntimeException 当文件读取失败或上传失败时抛出
 */
    @Override
    public DifyFileUploadResponse uploadFile(MultipartFile file, Long userId) {
        try {
        // 创建MultiValueMap对象，用于构建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // 将文件添加到请求体中，使用ByteArrayResource包装文件内容
            body.add("file", new ByteArrayResource(file.getBytes()) {
            // 重写getFilename方法，返回原始文件名
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
        // 将用户ID添加到请求体中
            body.add("user", String.valueOf(userId));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(knowledgeDifyProperties.getApiKey());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String uploadUrl = knowledgeDifyProperties.getApiUrl() + "/files/upload";

            ResponseEntity<DifyFileUploadResponse> response = restTemplate.postForEntity(
                    uploadUrl,
                    requestEntity,
                    DifyFileUploadResponse.class
            );

            DifyFileUploadResponse uploadResponse = response.getBody();
            if (uploadResponse == null) {
                throw new RuntimeException("Dify 文件上传失败：响应为空");
            }
            return uploadResponse;
        } catch (IOException e) {
            throw new RuntimeException("读取待上传文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DifyFileUploadResponse> uploadFiles(List<MultipartFile> files, Long userId) {
        List<DifyFileUploadResponse> responses = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return responses;
        }
        for (MultipartFile file : files) {
            try {
                responses.add(uploadFile(file, userId));
            } catch (RuntimeException ex) {
                log.error("[Knowledge Dify] 批量上传单个文件失败, fileName={}", file.getOriginalFilename(), ex);
            }
        }
        return responses;
    }

    @Override
    public List<DifyFileUploadResponse> uploadKnowledgeFiles(List<Long> fileIds, Long userId) {
        List<DifyFileUploadResponse> responses = new ArrayList<>();

        if (fileIds == null || fileIds.isEmpty()) {
            return responses;
        }

        for (Long fileId : fileIds) {
            try {
                boolean hasPermission = achievementFileService.hasFilePermission(fileId, userId);
                if (!hasPermission) {
                    log.warn("[Knowledge Dify] 用户无权限访问文件, fileId={}, userId={}", fileId, userId);
                    continue;
                }

                FileContextDTO context = achievementFileService.getFileContext(fileId);
                if (context == null || context.getFileUrl() == null) {
                    log.warn("[Knowledge Dify] 获取文件上下文失败或 URL 为空, fileId={}", fileId);
                    continue;
                }

                String fileUrl = context.getFileUrl();
                String fileName = context.getFileName();

                MultipartFile multipartFile = downloadAsMultipart(fileUrl, fileName);
                DifyFileUploadResponse response = uploadFile(multipartFile, userId);
                responses.add(response);
            } catch (Exception e) {
                log.error("[Knowledge Dify] 知识库文件上传处理失败, fileId={}", fileId, e);
            }
        }

        return responses;
    }

    private MultipartFile downloadAsMultipart(String fileUrl, String fileName) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = inputStream.readAllBytes();
            String safeFileName = (fileName != null && !fileName.isBlank()) ? fileName : "file";

            return new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getOriginalFilename() {
                    return safeFileName;
                }

                @Override
                public String getContentType() {
                    return MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }

                @Override
                public boolean isEmpty() {
                    return bytes.length == 0;
                }

                @Override
                public long getSize() {
                    return bytes.length;
                }

                @Override
                public byte[] getBytes() {
                    return bytes;
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public void transferTo(java.io.File dest) throws IOException {
                    java.nio.file.Files.write(dest.toPath(), bytes);
                }
            };
        }
    }
}
