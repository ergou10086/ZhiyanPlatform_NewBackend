package hbnu.project.zhiyanbackend.tasks.service.impl;

import hbnu.project.zhiyanbackend.oss.config.COSProperties;
import hbnu.project.zhiyanbackend.oss.dto.UploadFileResponseDTO;
import hbnu.project.zhiyanbackend.oss.service.COSService;
import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionFileResponse;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 基于COS的任务提交附件存储实现
 */
@Slf4j
@Service
public class TaskSubmissionFileServiceImpl implements TaskSubmissionFileService {

    private static final DateTimeFormatter FILENAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final COSService cosService;
    private final COSProperties cosProperties;
    private final long maxFileSize;

    public TaskSubmissionFileServiceImpl(
            COSService cosService,
            COSProperties cosProperties,
            @Value("${task-submission.files.max-size-bytes:104857600}") long maxFileSize) {
        this.cosService = cosService;
        this.cosProperties = cosProperties;
        this.maxFileSize = maxFileSize;
    }

    //这个方法
    @Override
    public TaskSubmissionFileResponse store(MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? sanitizeFilename(file.getOriginalFilename())
                : "file-" + System.currentTimeMillis();

        String extension = extractExtension(originalFilename);
        String cosFilename;
        String timeSuffix = LocalDateTime.now().format(FILENAME_TIME_FORMATTER);
        if (StringUtils.hasText(extension)) {
            cosFilename = "task-attachment-" + timeSuffix + "." + extension.toLowerCase(Locale.ROOT);
        } else {
            cosFilename = "task-attachment-" + timeSuffix;
        }

        try {
            // 使用 COS 简单上传接口，对于任务附件目前文件体积较小，不需要高级分块/断点续传
            UploadFileResponseDTO uploadResult = cosService.uploadFile(file, "task-submissions", cosFilename);
            return TaskSubmissionFileResponse.builder()
                    // 这里保存 COS 对象键，后续可用于生成预签名 URL
                    .url(uploadResult.getObjectKey())
                    .filename(cosFilename)
                    .contentType(uploadResult.getContentType())
                    .size(uploadResult.getSize())
                    .build();
        } catch (Exception e) {
            log.error("保存任务附件失败(上传COS): originalFilename={}, cosFilename={}, error={}", originalFilename, cosFilename, e.getMessage(), e);
            throw new IllegalArgumentException("保存附件失败: " + e.getMessage());
        }
    }

    @Override
    public List<TaskSubmissionFileResponse> storeBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<TaskSubmissionFileResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(store(file));
        }
        return responses;
    }

    @Override
    public boolean delete(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return false;
        }
        try {
            cosService.deleteObject(cosProperties.getBucketName(), relativePath);
            return true;
        } catch (Exception e) {
            log.warn("删除任务附件失败(COS): {}", relativePath, e);
            return false;
        }
    }

    @Override
    public int deleteBatch(List<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (String path : relativePaths) {
            if (delete(path)) {
                success++;
            }
        }
        return success;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小不能超过 " + (maxFileSize / (1024 * 1024)) + "MB");
        }
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf(".");
        if (idx == -1 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1);
    }

    private String sanitizeFilename(String filename) {
        String clean = Normalizer.normalize(filename, Normalizer.Form.NFKC);
        clean = clean.replaceAll("[\\r\\n]", "");
        clean = clean.replaceAll("[/\\\\]", "_");
        return clean.trim();
    }
}


