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

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAttachmentService {

    private final DifyFileService difyFileService;

    public List<String> downloadAndUploadAttachments(List<String> attachmentUrls, Long userId) {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> difyFileIds = new ArrayList<>();
        for (String url : attachmentUrls) {
            try {
                MultipartFile multipartFile = downloadAsMultipart(url);
                DifyFileUploadResponse response = difyFileService.uploadFile(multipartFile, userId);
                if (response != null && response.getFileId() != null) {
                    difyFileIds.add(response.getFileId());
                }
            } catch (Exception e) {
                log.error("处理附件失败, url={}", url, e);
            }
        }
        return difyFileIds;
    }

    private MultipartFile downloadAsMultipart(String fileUrl) throws IOException, URISyntaxException {
        URI uri = new URI(fileUrl);
        URL url = uri.toURL();
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = inputStream.readAllBytes();
            String fileName = extractFileNameFromUrl(fileUrl);

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

    private String extractFileNameFromUrl(String url) {
        int idx = url.lastIndexOf('/');
        if (idx >= 0 && idx < url.length() - 1) {
            return url.substring(idx + 1);
        }
        return "file";
    }
}
