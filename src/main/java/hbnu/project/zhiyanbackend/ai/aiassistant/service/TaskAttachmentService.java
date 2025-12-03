package hbnu.project.zhiyanbackend.ai.aiassistant.service;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 任务附件服务类
 * 提供附件下载和上传功能
 *
 * @author Tokito
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAttachmentService {

    private final DifyFileService difyFileService;

    /**
     * 下载并上传附件
     * @param attachmentUrls 附件URL列表
     * @param userId 用户ID
     * @return 上传成功后的文件ID列表
     */
    public List<String> downloadAndUploadAttachments(List<String> attachmentUrls, Long userId) {
        // 如果附件列表为空，直接返回空列表
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return Collections.emptyList();
        }

        // 存储上传成功的文件ID
        List<String> difyFileIds = new ArrayList<>();
        // 遍历所有附件URL进行处理
        for (String url : attachmentUrls) {
            try {
                // 将URL下载为MultipartFile对象
                MultipartFile multipartFile = downloadAsMultipart(url);
                // 上传文件到Dify服务
                DifyFileUploadResponse response = difyFileService.uploadFile(multipartFile, userId);
                // 如果上传成功，记录文件ID
                if (response != null && response.getFileId() != null) {
                    difyFileIds.add(response.getFileId());
                }
            } catch (Exception e) {
                // 记录处理附件失败日志
                log.error("处理附件失败, url={}", url, e);
            }
        }
        return difyFileIds;
    }

    /**
     * 从URL下载文件并转换为MultipartFile对象
     * @param fileUrl 文件URL
     * @return MultipartFile对象
     * @throws IOException IO异常
     * @throws URISyntaxException URI语法异常
     */
    private MultipartFile downloadAsMultipart(String fileUrl) throws IOException, URISyntaxException {
        // 解析URL
        URI uri = new URI(fileUrl);
        URL url = uri.toURL();
        // 从URL读取文件内容
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = inputStream.readAllBytes();
            // 从URL中提取文件名
            String fileName = extractFileNameFromUrl(fileUrl);

            // 创建并返回MultipartFile对象
            return new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getOriginalFilename() {
                    return fileName;
                }

                @Override
                public String getContentType() {
                    // 从文件扩展名确定内容类型
                    String extension = fileName.contains(".")
                            ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
                            : "";
                    return switch (extension) {
                        case "pdf" -> "application/pdf";
                        case "doc", "docx" -> "application/msword";
                        case "xls", "xlsx" -> "application/vnd.ms-excel";
                        case "png" -> "image/png";
                        case "jpg", "jpeg" -> "image/jpeg";
                        case "txt" -> "text/plain";
                        default -> "application/octet-stream";
                    };
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
                    Files.write(dest.toPath(), bytes);
                }
            };
        }
    }

    /**
     * 从URL中提取文件名
     * @param url 文件URL
     * @return 文件名
     */
    private String extractFileNameFromUrl(String url) {
        int idx = url.lastIndexOf('/');
        if (idx >= 0 && idx < url.length() - 1) {
            return url.substring(idx + 1);
        }
        return "file";
    }
}
