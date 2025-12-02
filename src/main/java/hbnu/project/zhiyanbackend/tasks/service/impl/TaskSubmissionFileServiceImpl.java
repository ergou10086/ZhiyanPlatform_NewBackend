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
import java.util.UUID;

/**
 * 基于本地文件系统的任务提交附件存储实现
 *
 * @author yxy
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
            log.info("保存任务附件: originalFilename={}, newFilename={}, destinationFile={}, size={}",
                    originalFilename, newFilename, destinationFile, file.getSize());
            file.transferTo(destinationFile);
            
            // 验证文件是否真的被保存
            if (!Files.exists(destinationFile)) {
                log.error("文件保存后不存在: {}", destinationFile);
                throw new IOException("文件保存失败，文件不存在");
            }
            long savedSize = Files.size(destinationFile);
            log.info("任务附件保存成功: destinationFile={}, savedSize={}, expectedSize={}",
                    destinationFile, savedSize, file.getSize());
            
            String relativePath = datePath + "/" + newFilename;
            return TaskSubmissionFileResponse.builder()
                    .url(relativePath.replace("\\", "/"))
                    .filename(originalFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            log.error("保存任务附件失败: originalFilename={}, error={}", originalFilename, e.getMessage(), e);
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
            log.debug("加载附件资源: relativePath={}, resolvedPath={}", relativePath, filePath);
            
            // 检查文件是否存在（使用 Files.exists 更可靠）
            if (!Files.exists(filePath)) {
                log.warn("附件文件不存在: relativePath={}, resolvedPath={}", relativePath, filePath);
                // 列出目录内容以便调试
                Path parentDir = filePath.getParent();
                if (parentDir != null && Files.exists(parentDir)) {
                    try {
                        List<String> filesInDir = Files.list(parentDir)
                                .filter(Files::isRegularFile)
                                .map(p -> p.getFileName().toString())
                                .toList();
                        log.warn("目录 {} 中的文件: {}", parentDir, filesInDir);
                        
                        // 尝试查找同扩展名的文件（容错处理，可能是历史数据问题）
                        String requestedExtension = extractExtension(relativePath);
                        if (StringUtils.hasText(requestedExtension)) {
                            List<Path> matchingFiles = Files.list(parentDir)
                                    .filter(Files::isRegularFile)
                                    .filter(p -> {
                                        String fileName = p.getFileName().toString();
                                        String ext = extractExtension(fileName);
                                        return requestedExtension.equalsIgnoreCase(ext);
                                    })
                                    .toList();
                            
                            if (!matchingFiles.isEmpty()) {
                                // 如果只有一个匹配的文件，尝试使用它（可能是文件名不匹配但扩展名相同）
                                if (matchingFiles.size() == 1) {
                                    Path alternativeFile = matchingFiles.get(0);
                                    log.warn("找到同扩展名的替代文件: 请求文件={}, 替代文件={}", 
                                            filePath.getFileName(), alternativeFile.getFileName());
                                    // 注意：这里不自动使用替代文件，因为可能不安全
                                    // 只是记录日志，让用户知道有类似的文件存在
                                } else {
                                    log.warn("找到 {} 个同扩展名的文件，无法确定使用哪个", matchingFiles.size());
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.debug("无法列出目录内容: {}", parentDir, e);
                    }
                } else {
                    log.warn("父目录不存在: {}", parentDir);
                }
                return null;
            }
            
            if (!Files.isReadable(filePath)) {
                log.warn("附件文件不可读: relativePath={}, resolvedPath={}", relativePath, filePath);
                return null;
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                log.info("附件资源加载成功: relativePath={}, size={}", relativePath, resource.contentLength());
                return resource;
            }
            log.warn("附件资源不存在或不可读: relativePath={}, resolvedPath={}, exists={}, readable={}",
                    relativePath, filePath, resource.exists(), resource.isReadable());
            return null;
        } catch (MalformedURLException e) {
            log.error("加载附件资源失败: relativePath={}", relativePath, e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("路径解析失败: relativePath={}, error={}", relativePath, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("加载附件资源时发生异常: relativePath={}", relativePath, e);
            return null;
        }
    }

    private Path resolvePath(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("相对路径不能为空");
        }
        String cleaned = relativePath.replace("\\", "/");
        // 移除开头的斜杠，确保是相对路径
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        // 直接使用 rootLocation.resolve(String)，Path.resolve 方法可以正确处理相对路径字符串
        Path resolved = rootLocation.resolve(cleaned).normalize();
        if (!resolved.startsWith(rootLocation)) {
            log.error("非法的文件路径: relativePath={}, resolved={}, rootLocation={}", 
                    relativePath, resolved, rootLocation);
            throw new IllegalArgumentException("非法的文件路径: " + relativePath);
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


