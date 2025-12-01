package hbnu.project.zhiyanbackend.ai.aiassistant.service.impl;

import hbnu.project.zhiyanbackend.ai.aiassistant.config.DifyProperties;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.DifyFileService;
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
public class DifyFileServiceImpl implements DifyFileService {

    private final DifyProperties difyProperties;
    private final RestTemplate restTemplate;
    private final AchievementFileService achievementFileService;

    @Override
    public DifyFileUploadResponse uploadFile(MultipartFile file, Long userId) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("user", String.valueOf(userId));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(difyProperties.getApiKey());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String uploadUrl = difyProperties.getApiUrl() + "/files/upload";

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
        for (MultipartFile file : files) {
            try {
                responses.add(uploadFile(file, userId));
            } catch (RuntimeException ex) {
                log.error("Dify 批量上传单个文件失败, fileName={}", file.getOriginalFilename(), ex);
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
                // 权限检查
                boolean hasPermission = achievementFileService.hasFilePermission(fileId, userId);
                if (!hasPermission) {
                    log.warn("[Dify 知识库上传] 用户无权限访问文件, fileId={}, userId={}", fileId, userId);
                    continue;
                }

                // 获取文件上下文（包含预签名 URL 或 COS 公网 URL）
                FileContextDTO context = achievementFileService.getFileContext(fileId);
                if (context == null || context.getFileUrl() == null) {
                    log.warn("[Dify 知识库上传] 获取文件上下文失败或 URL 为空, fileId={}", fileId);
                    continue;
                }

                String fileUrl = context.getFileUrl();
                String fileName = context.getFileName();

                MultipartFile multipartFile = downloadAsMultipart(fileUrl, fileName);
                DifyFileUploadResponse response = uploadFile(multipartFile, userId);
                responses.add(response);
            } catch (Exception e) {
                log.error("[Dify 知识库上传] 处理单个文件失败, fileId={}", fileId, e);
            }
        }

        return responses;
    }

     /**
      * 将远程文件下载为 MultipartFile，方便复用现有的 Dify 上传逻辑
      */
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
