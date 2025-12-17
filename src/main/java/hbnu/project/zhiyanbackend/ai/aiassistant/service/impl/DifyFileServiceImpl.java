package hbnu.project.zhiyanbackend.ai.aiassistant.service.impl;

import hbnu.project.zhiyanbackend.ai.aiassistant.config.DifyProperties;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.DifyFileService;
import hbnu.project.zhiyanbackend.basic.exception.DifyException;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * dify文件的服务实现
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DifyFileServiceImpl implements DifyFileService {

    private final DifyProperties difyProperties;
    private final RestTemplate restTemplate;
    private final AchievementFileService achievementFileService;

    /**
     * 上传单个文件到dify
     *
     * @param file 要上传的文件，类型为MultipartFile
     * @param userId 用户ID，用于标识文件所属用户
     * @return DifyFileUploadResponse 上传响应
     */
    @Override
    public DifyFileUploadResponse uploadFile(MultipartFile file, Long userId) {
        // 1. 入参校验
        ValidationUtils.requireNonEmptyFile(file);
        ValidationUtils.requireId(userId, "userId");

        try {
            // 2. 构建请求体
            MultiValueMap<String, Object> body = buildUploadRequestBody(file, userId);
            // 3. 构建请求头
            HttpHeaders headers = buildUploadRequestHeaders();
            // 4. 构建请求实体
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            // 5. 拼接上传URL（防止配置的URL末尾有/导致拼接错误）
            String uploadUrl = buildUploadUrl();
            // 6. 发送请求并处理响应
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

    /**
     * 批量上传文件到dify
     * @param files 要上传的文件列表，类型为List<MultipartFile>
     * @param userId 用户ID，用于标识文件所属用户
     * @return DifyFileUploadResponse
     */
    @Override
    public List<DifyFileUploadResponse> uploadFiles(List<MultipartFile> files, Long userId) {
        ValidationUtils.requireId(userId, "userId");
        ValidationUtils.requireNonEmptyFileList(files, "files");

        List<DifyFileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                responses.add(uploadFile(file, userId));
            } catch (DifyException ex) {
                log.error("Dify 批量上传单个文件失败, fileName={}", file.getOriginalFilename(), ex);
            }
        }
        return responses;
    }

    /**
     * 上传知识库的文件到dify
     * 可批量
     *
     * @param fileIds 文件ID列表，用于标识要上传的知识文件
     * @param userId 用户ID，用于标识文件所属用户
     * @return DifyFileUploadResponse的列表
     */
    @Override
    public List<DifyFileUploadResponse> uploadKnowledgeFiles(List<Long> fileIds, Long userId) {
        ValidationUtils.requireId(userId, "userId");
        ValidationUtils.requireNonEmpty(fileIds, "fileIds");

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
                
                log.info("[Dify 知识库上传] 准备下载文件: fileId={}, fileName={}, fileUrl前50字符={}", 
                        fileId, fileName, fileUrl.substring(0, Math.min(50, fileUrl.length())));

                MultipartFile multipartFile = downloadAsMultipart(fileUrl, fileName);
                log.info("[Dify 知识库上传] 文件下载成功: fileId={}, fileName={}, size={}字节", 
                        fileId, fileName, multipartFile.getSize());
                
                DifyFileUploadResponse response = uploadFile(multipartFile, userId);
                log.info("[Dify 知识库上传] 文件上传到Dify成功: fileId={}, difyFileId={}", 
                        fileId, response != null ? response.getFileId() : "null");
                responses.add(response);
            } catch (DifyException e) {
                log.error("[Dify 知识库上传] 处理单个文件失败, fileId={}", fileId, e);
            } catch (IOException e) {
                log.error("[Dify 知识库上传] 处理单个文件失败, 其他错误, fileId={}", fileId);
            }
        }

        return responses;
    }

     /**
      * 将远程文件下载为 MultipartFile，方便复用现有的 Dify 上传逻辑
      */
     private MultipartFile downloadAsMultipart(String fileUrl, String fileName) throws IOException {
         URI uri = URI.create(fileUrl);
         URL url = uri.toURL();

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
                 public void transferTo(File dest) throws IOException {
                     java.nio.file.Files.write(dest.toPath(), bytes);
                 }
             };
         }
     }

    /**
     * 构建文件上传的请求体
     */
    private MultiValueMap<String, Object> buildUploadRequestBody(MultipartFile file, Long userId) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // 使用ByteArrayResource包装文件字节，指定文件名
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });
        body.add("user", String.valueOf(userId));
        return body;
    }

    /**
     * 构建文件上传的请求头
     */
    private HttpHeaders buildUploadRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // 校验API Key是否配置
        if (difyProperties.getApiKey() == null || difyProperties.getApiKey().isBlank()) {
            throw new DifyException("Dify配置异常：API Key未配置");
        }
        headers.setBearerAuth(difyProperties.getApiKey());
        return headers;
    }

    /**
     * 拼接上传URL，处理URL末尾的/问题
     */
    private String buildUploadUrl() {
        String apiUrl = difyProperties.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new DifyException("Dify配置异常：API URL未配置");
        }
        // 统一URL拼接规则：如果apiUrl末尾有/，则直接拼接，否则加/后拼接
        return apiUrl.endsWith("/") ? apiUrl + "files/upload" : apiUrl + "/files/upload";
    }
}
