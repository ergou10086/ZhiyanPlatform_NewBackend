package hbnu.project.zhiyanbackend.tasks.service.impl;

import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionFileResponse;
import hbnu.project.zhiyanbackend.tasks.service.TaskSubmissionFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于本地文件系统的任务提交附件存储实现
 */
@Slf4j
@Service
public class TaskSubmissionFileServiceImpl implements TaskSubmissionFileService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path rootLocation;
    private final long maxFileSize;

    public TaskSubmissionFileServiceImpl(
            @Value("${task-submission.files.storage-path:uploads/task-submissions}") String storagePath,
            @Value("${task-submission.files.max-size-bytes:104857600}") long maxFileSize) throws IOException {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        Files.createDirectories(this.rootLocation);
        log.info("任务附件存储目录: {}", this.rootLocation);
    }

    @Override
    public TaskSubmissionFileResponse store(MultipartFile file) {
        validateFile(file);
        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? sanitizeFilename(file.getOriginalFilename())
                : "file-" + System.currentTimeMillis();
        String extension = extractExtension(originalFilename);
        String randomName = UUID.randomUUID().toString().replace("-", "");
        String newFilename = extension.isEmpty() ? randomName : randomName + "." + extension;

        String datePath = LocalDate.now().format(DATE_FORMATTER);
        Path destinationDir = rootLocation.resolve(datePath).normalize();
        try {
            Files.createDirectories(destinationDir);
            Path destinationFile = destinationDir.resolve(newFilename).normalize();
            if (!destinationFile.startsWith(rootLocation)) {
                throw new IllegalArgumentException("非法的文件路径");
            }
            file.transferTo(destinationFile);
            String relativePath = datePath + "/" + newFilename;
            return TaskSubmissionFileResponse.builder()
                    .url(relativePath.replace("\\", "/"))
                    .filename(originalFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            log.error("保存任务附件失败: {}", e.getMessage(), e);
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
            Path filePath = resolvePath(relativePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除任务附件失败: {}", relativePath, e);
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

    @Override
    public Resource loadAsResource(String relativePath) {
        try {
            Path filePath = resolvePath(relativePath);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            return null;
        } catch (MalformedURLException e) {
            log.error("加载附件资源失败: {}", relativePath, e);
            return null;
        }
    }

    private Path resolvePath(String relativePath) {
        String cleaned = relativePath.replace("\\", "/");
        Path path = Paths.get(cleaned).normalize();
        Path resolved = rootLocation.resolve(path).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        return resolved;
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


