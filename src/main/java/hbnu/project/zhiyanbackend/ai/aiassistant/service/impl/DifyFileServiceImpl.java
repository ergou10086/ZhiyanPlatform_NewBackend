package hbnu.project.zhiyanbackend.ai.aiassistant.service.impl;

import hbnu.project.zhiyanbackend.ai.aiassistant.config.DifyProperties;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.DifyFileService;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DifyFileServiceImpl implements DifyFileService {

    private final DifyProperties difyProperties;
    private final RestTemplate restTemplate;

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
        throw new UnsupportedOperationException("知识库模块尚未迁移，暂不支持从知识库上传文件到 Dify");
    }
}
