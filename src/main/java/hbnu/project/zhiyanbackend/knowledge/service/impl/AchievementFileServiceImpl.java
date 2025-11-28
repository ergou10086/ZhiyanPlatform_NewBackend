package hbnu.project.zhiyanbackend.knowledge.service.impl;

import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.DeleteObjectsResult;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.FileUtils;
import hbnu.project.zhiyanbackend.knowledge.model.converter.AchievementFileConverter;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.FileContextDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.UploadFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementFileRepository;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementFileService;
import hbnu.project.zhiyanbackend.message.service.impl.MessageSendServiceImpl;
import hbnu.project.zhiyanbackend.oss.config.COSProperties;
import hbnu.project.zhiyanbackend.oss.dto.UploadFileResponseDTO;
import hbnu.project.zhiyanbackend.oss.service.COSService;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 成果文件服务实现
 * 成果上传的各种服务实现，数据写入到mysql，文件上传到minio等
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementFileServiceImpl implements AchievementFileService {

    private final COSService cosService;
    private final COSProperties cosProperties;
    private final AchievementFileRepository achievementFileRepository;
    private final AchievementRepository achievementRepository;
    private final ProjectMemberService projectMemberService;
    private final AchievementFileConverter achievementFileConverter;
    private final MessageSendServiceImpl knowledgeMessageService;
    private final UserRepository userRepository;

    /**
     * 默认预签名URL过期时间（3天）
     */
    private static final int DEFAULT_EXPIRY_SECONDS = 3 * 24 * 3600;

    /**
     * 上传成果文件
     *
     * @param file      文件
     * @param uploadDTO 上传DTO
     * @return 文件信息
     */
    @Override
    @Transactional
    public AchievementFileDTO uploadFile(MultipartFile file, UploadFileDTO uploadDTO) {
        log.info("开始上传成果文件: achievementId={}, fileName={}",
                uploadDTO.getAchievementId(), file.getOriginalFilename());

        // 1. 验证是否存在成果
        Achievement achievement = achievementRepository.findById(uploadDTO.getAchievementId())
                .orElseThrow(() -> new ServiceException("成果不存在"));

        // 2.获取文件信息
        String originalFilename = file.getOriginalFilename();
        if(StringUtils.isEmpty(originalFilename)){
            throw new ServiceException("文件名不能为空");
        }
        // 获取扩展名
        String fileExtension = FileUtils.getExtension(file);
        long fileSize = file.getSize();

        // 3. 检查是否存在同名文件，如果存在则删除旧文件（覆盖模式）
        Optional<AchievementFile> existingFile = achievementFileRepository
                .findByAchievementIdAndFileName(uploadDTO.getAchievementId(), originalFilename);

        if (existingFile.isPresent()) {
            AchievementFile oldFile = existingFile.get();
            log.info("检测到同名文件，执行覆盖删除: fileId={}, objectKey={}",
                    oldFile.getId(), oldFile.getObjectKey());

            try {
                cosService.deleteObject(cosProperties.getBucketName(), oldFile.getObjectKey());
                log.info("COS旧文件删除成功: objectKey={}", oldFile.getObjectKey());
                achievementFileRepository.deleteById(oldFile.getId());
                log.info("数据库旧文件记录删除成功: fileId={}", oldFile.getId());
            } catch (Exception e) {
                log.error("COS旧文件删除失败: objectKey={}", oldFile.getObjectKey(), e);
                // COS删除失败不更新数据库，避免悬空
            }
        }

        // 4. 使用COS高级接口上传文件（自动处理分片）
        UploadFileResponseDTO uploadResult;
        try {
            uploadResult = cosService.uploadFileSenior(file, "achievement", null);
        } catch (Exception e) {
            log.error("文件上传COS失败", e);
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }

        // 5. 保存文件记录到数据库
        AchievementFile achievementFile = AchievementFile.builder()
                .achievementId(uploadDTO.getAchievementId())
                .fileName(originalFilename)
                .fileSize(fileSize)
                .fileType(fileExtension)
                .bucketName(cosProperties.getBucketName())
                .objectKey(uploadResult.getObjectKey())
                // 保留字段名，实际存储COS URL
                .cosUrl(uploadResult.getUrl())
                .uploadBy(uploadDTO.getUploadBy())
                .uploadAt(LocalDateTime.now())
                .build();

        achievementFile = achievementFileRepository.save(achievementFile);

        log.info("文件上传成功: fileId={}, objectKey={}", achievementFile.getId(), uploadResult.getObjectKey());

        // 6. 发送文件上传通知给项目成员（除了上传者自己）
        knowledgeMessageService.notifyAchievementFileUpload(achievement, achievementFile, uploadDTO.getUploadBy());

        // 7. 转换为DTO返回
        return achievementFileConverter.toDTO(achievementFile);
    }

    /**
     * 批量上传成果文件
     *
     * @param files        文件列表
     * @param achievementId 成果ID
     * @param uploadBy      上传者ID
     * @return 文件信息列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AchievementFileDTO> uploadFilesBatch(MultipartFile[] files, Long achievementId, Long uploadBy) {
        if (files == null) {
            throw new ServiceException("文件列表不能为空");
        }

        List<AchievementFileDTO> results = new ArrayList<>();
        // 业务类型为achievement，成果文件
        String businessType = "achievement";
        try {
            cosService.uploadBatch(files, businessType);
        }catch (Exception e){
            log.error("批量上传文件出现问题", e);
            // 继续处理其他文件，不阻塞
        }

        return results;
    }

    /**
     * 获取文件预签名URL
     *
     * @param fileId       文件ID
     * @param userId       当前用户ID
     * @param expirySeconds 过期时间（秒）
     * @return 预签名URL
     */
    @Override
    public String generatePresignedUrl(Long fileId, Long userId, Integer expirySeconds) {
        log.info("生成文件下载链接: fileId={}, userId={}", fileId, userId);

        // 1. 查询文件是否存在
        AchievementFile file = achievementFileRepository.findById(fileId)
                .orElseThrow(() -> new ServiceException("文件不存在"));

        log.info("文件信息: fileName={}, objectKey={}, bucketName={}",
                file.getFileName(), file.getObjectKey(), file.getBucketName());

        // 2. 验证权限
        if (!hasFilePermission(fileId, userId)) {
            log.warn("用户无权限访问文件: fileId={}, userId={}", fileId, userId);
            throw new ServiceException("无权限访问该文件");
        }

        // 3. 生成预签名URL
        int expiry = expirySeconds != null ? expirySeconds : DEFAULT_EXPIRY_SECONDS;
        try {
            log.info("开始生成预签名URL: bucketName={}, objectKey={}, expiry={}s",
                    cosProperties.getBucketName(), file.getObjectKey(), expiry);

            Date expiration = new Date(System.currentTimeMillis() + expiry * 1000L);
            URL url = cosService.generatePresignedUrl(
                    cosProperties.getBucketName(),
                    file.getObjectKey(),
                    expiration,
                    HttpMethodName.GET,
                    null,
                    null,
                    false,
                    true
            );

            if (url == null || url.toString().isEmpty()) {
                log.error("生成的预签名URL为空: fileId={}", fileId);
                throw new ServiceException("生成的预签名URL为空");
            }

            log.info("生成文件预签名URL成功: fileId={}, expiry={}s", fileId, expiry);
            return url.toString();
        } catch (Exception e) {
            log.error("生成文件预签名URL失败: fileId={}, exceptionType={}, message={}",
                    fileId, e.getClass().getName(), e.getMessage(), e);
            throw new ServiceException("生成预签名URL失败: " + e.getMessage());
        }
    }

    /**
     * 下载成果文件（通过预签名URL重定向）
     * 逻辑规范接口
     *
     * @param fileId 文件ID
     * @param userId 当前用户ID（用于权限验证）
     */
    @Override
    public void downloadFile(Long fileId, Long userId) {
        // 生成预签名URL并重定向
        String presignedUrl = generatePresignedUrl(fileId, userId, DEFAULT_EXPIRY_SECONDS);
        // 注意：此方法需要Controller层配合实现重定向
        // 或者返回URL让前端处理
        throw new ServiceException("请使用generatePresignedUrl获取下载链接");
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param userId 当前用户ID（用于权限验证）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId, Long userId) {
        log.info("开始删除文件: fileId={}, userId={}", fileId, userId);

        // 1. 查询文件记录
        AchievementFile file = achievementFileRepository.findById(fileId)
                .orElseThrow(() -> new ServiceException("文件不存在"));

        // 2. 查询成果信息
        Achievement achievement = achievementRepository.findById(file.getAchievementId())
                .orElseThrow(() -> new ServiceException("成果不存在"));

        // 3. 验证权限
        if (!hasFilePermission(fileId, userId)) {
            throw new ServiceException("无权限删除该文件");
        }

        // 4. 从COS删除文件
        try {
            cosService.deleteObject(cosProperties.getBucketName(), file.getObjectKey());
            log.info("COS文件删除成功: objectKey={}", file.getObjectKey());

            // 5. 删除数据库记录
            achievementFileRepository.deleteById(fileId);
            log.info("文件删除成功: fileId={}", fileId);

            // 6. 发送成果文件删除的通知
            knowledgeMessageService.notifyAchievementFileDeleted(achievement, file, userId);
        } catch (Exception e) {
            log.error("COS文件删除失败: objectKey={}", file.getObjectKey(), e);
            // COS删除失败，不继续删除数据库记录，避免悬空
            throw new ServiceException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除文件
     * 调cos的批量删除服务
     *
     * @param fileIds 文件ID列表
     * @param userId  当前用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFiles(List<Long> fileIds, Long userId) {
        log.info("开始批量删除文件: fileIds={}, userId={}", fileIds, userId);

        if (fileIds == null || fileIds.isEmpty()) {
            log.warn("批量删除文件失败: 文件ID列表为空");
            throw new ServiceException("文件ID列表不能为空");
        }

        if (userId == null) {
            log.warn("批量删除文件失败: 用户未登录");
            throw new ServiceException("用户未登录");
        }

        // 1. 查询所有文件记录
        List<AchievementFile> files = achievementFileRepository.findAllById(fileIds);

        if (files.isEmpty()) {
            log.warn("批量删除文件失败: 未找到任何文件记录");
            throw new ServiceException("未找到指定的文件");
        }

        // 2. 验证权限和收集数据
        Map<Long, Achievement> achievementMap = new HashMap<>();
        List<String> objectKeys = new ArrayList<>();
        List<Long> validFileIds = new ArrayList<>();

        for (AchievementFile file : files) {
            try {
                // 验证权限
                if (!hasFilePermission(file.getId(), userId)) {
                    log.warn("用户无权限删除文件: fileId={}, userId={}", file.getId(), userId);
                    continue; // 跳过无权限的文件
                }

                // 查询成果信息（缓存避免重复查询）
                if (!achievementMap.containsKey(file.getAchievementId())) {
                    achievementRepository.findById(file.getAchievementId()).ifPresent(achievement -> achievementMap.put(file.getAchievementId(), achievement));
                }

                objectKeys.add(file.getObjectKey());
                validFileIds.add(file.getId());

            } catch (Exception e) {
                log.error("处理文件权限验证时出错: fileId={}", file.getId(), e);
                // 继续处理其他文件
            }
        }

        if (objectKeys.isEmpty()) {
            log.warn("批量删除文件失败: 没有可删除的有效文件");
            throw new ServiceException("没有可删除的文件或无操作权限");
        }

        // 3. 批量删除COS文件
        try {
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(cosProperties.getBucketName());

            // 构建要删除的key列表
            List<DeleteObjectsRequest.KeyVersion> keys = objectKeys.stream()
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .collect(java.util.stream.Collectors.toList());

            deleteRequest.setKeys(keys);
            deleteRequest.setQuiet(false); // 设置为false可以获取详细的删除结果

            log.info("开始批量删除COS文件: bucket={}, count={}",
                    cosProperties.getBucketName(), objectKeys.size());

            // 执行批量删除
            DeleteObjectsResult deleteResult = cosService.deleteObjects(deleteRequest);

            // 处理删除结果
            List<DeleteObjectsResult.DeletedObject> deletedObjects = deleteResult.getDeletedObjects();
            if (deletedObjects != null) {
                log.info("COS批量删除成功: 成功删除 {} 个文件", deletedObjects.size());

                // 记录删除成功的文件
                for (DeleteObjectsResult.DeletedObject deleted : deletedObjects) {
                    log.debug("COS文件删除成功: key={}", deleted.getKey());
                }
            }

            // 4. 删除数据库记录
            achievementFileRepository.deleteAllById(validFileIds);
            log.info("数据库文件记录删除成功: 删除了 {} 个文件记录", validFileIds.size());

            // 5. 发送批量删除通知
            knowledgeMessageService.notifyAchievementFilesBatchDeleted(achievementMap, files, userId);

            log.info("批量删除文件完成: 成功删除 {} 个文件", validFileIds.size());

        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            // COS删除失败，不删除数据库记录，避免悬空
            throw new ServiceException("批量删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 根据成果ID批量删除文件
     * 用于删除整个成果时调用
     *
     * @param achievementId 成果ID
     * @param userId 操作者ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFilesByAchievementId(Long achievementId, Long userId) {
        log.info("开始删除成果的所有文件: achievementId={}, userId={}", achievementId, userId);

        // 1. 查询成果的所有文件
        List<AchievementFile> files = achievementFileRepository.findByAchievementId(achievementId);

        if (files.isEmpty()) {
            log.info("成果没有文件可删除: achievementId={}", achievementId);
            return;
        }

        // 2. 查询成果信息
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在"));

        // 3. 验证权限
        if (!projectMemberService.isMember(achievement.getProjectId(), userId)) {
            throw new ServiceException("无权限删除该成果的文件");
        }

        // 4. 收集要删除的objectKey
        List<String> objectKeys = files.stream()
                .map(AchievementFile::getObjectKey)
                .toList();

        List<Long> fileIds = files.stream()
                .map(AchievementFile::getId)
                .collect(java.util.stream.Collectors.toList());

        // 5. 批量删除COS文件
        try {
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(cosProperties.getBucketName());
            List<DeleteObjectsRequest.KeyVersion> keys = objectKeys.stream()
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .collect(java.util.stream.Collectors.toList());

            deleteRequest.setKeys(keys);
            // 静默模式，不返回详细结果
            deleteRequest.setQuiet(true);

            cosService.deleteObjects(deleteRequest);
            log.info("COS批量删除成功: 删除了 {} 个文件", objectKeys.size());

            // 6. 删除数据库记录
            achievementFileRepository.deleteAllById(fileIds);
            log.info("数据库文件记录删除成功: 删除了 {} 个文件记录", fileIds.size());

            // 7. 发送成果文件全部删除通知
            knowledgeMessageService.notifyAchievementAllFilesDeleted(achievement, files, userId);

            log.info("成果文件批量删除完成: achievementId={}, fileCount={}",
                    achievementId, files.size());

        } catch (Exception e) {
            log.error("批量删除成果文件失败: achievementId={}", achievementId, e);
            throw new ServiceException("批量删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 查询成果的所有文件
     *
     * @param achievementId 成果ID
     * @return 文件列表
     */
    @Override
    public List<AchievementFileDTO> getFilesByAchievementId(Long achievementId) {
        List<AchievementFile> files = achievementFileRepository.findByAchievementId(achievementId);

        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        return achievementFileConverter.toDTOList(files);
    }

    /**
     * 根据ID获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件DTO
     */
    @Override
    public AchievementFileDTO getFileById(Long fileId) {
        AchievementFile achievementFile = achievementFileRepository.findById(fileId)
                .orElseThrow(() -> new ServiceException("文件不存在"));
        return achievementFileConverter.toDTO(achievementFile);
    }

    /**
     * 统计成果的文件数量
     *
     * @param achievementId 成果ID
     * @return 文件数量
     */
    @Override
    public long countFilesByAchievementId(Long achievementId) {
        return achievementFileRepository.countByAchievementId(achievementId);
    }

    /**
     * 检查用户是否有权限访问文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    @Override
    public boolean hasFilePermission(Long fileId, Long userId) {
        if (userId == null) {
            log.warn("权限检查失败: 用户未登录, fileId={}", fileId);
            return false;
        }

        try {
            // 查询文件信息
            AchievementFile file = achievementFileRepository.findById(fileId)
                    .orElse(null);

            if (file == null) {
                log.warn("权限检查失败: 文件不存在, fileId={}", fileId);
                return false;
            }

            // 查询成果信息
            Achievement achievement = achievementRepository.findById(file.getAchievementId())
                    .orElse(null);

            if (achievement == null) {
                log.warn("权限检查失败: 成果不存在, achievementId={}", file.getAchievementId());
                return false;
            }

            // 检查用户是否为项目成员
            boolean isMember = projectMemberService.isMember(achievement.getProjectId(), userId);

            if (!isMember) {
                log.warn("权限检查失败: 用户不是项目成员, fileId={}, userId={}, projectId={}",
                        fileId, userId, achievement.getProjectId());
                return false;
            }

            log.debug("权限检查通过: fileId={}, userId={}", fileId, userId);
            return true;
        } catch (Exception e) {
            log.error("权限检查异常: fileId={}, userId={}", fileId, userId, e);
            return false;
        }
    }

    /**
     * 获取文件上下文（用于 AI 对话）
     *
     * @param fileId 文件 ID
     * @return 文件上下文信息
     */
    @Override
    public FileContextDTO getFileContext(Long fileId) {
        log.info("[文件上下文] 获取文件信息: fileId={}", fileId);

        AchievementFile file = achievementFileRepository.findById(fileId)
                .orElse(null);

        // 获取上传用户信息
        Long uploadUserId = Objects.requireNonNull(file).getUploadBy();
        String uploadUserName;
        if (uploadUserId != null) {
            Optional<String> userNameOptional = userRepository.findNameById(uploadUserId);
            uploadUserName = userNameOptional.orElse("未知用户");
        } else {
            uploadUserName = "未知用户";
        }

        // 防爆刷
        if (file == null) {
            log.warn("[文件上下文] 文件不存在: fileId={}", fileId);
            return null;
        }

        // 生成预签名 URL（使用COS服务）
        String fileUrl;
        try {
            Date expiration = new Date(System.currentTimeMillis() + DEFAULT_EXPIRY_SECONDS * 1000L);
            URL url = cosService.generatePresignedUrl(
                    cosProperties.getBucketName(),
                    file.getObjectKey(),
                    expiration,
                    HttpMethodName.GET,
                    null,
                    null,
                    false,
                    true
            );
            fileUrl = url != null ? url.toString() : null;
        } catch (Exception e) {
            log.error("[文件上下文] 生成文件 URL 失败: fileId={}", fileId, e);
            // 失败时使用 COS 公网 URL 作为备用
            // 保留字段名，实际存储COS URL
            fileUrl = file.getCosUrl();
        }

        // 格式化文件大小
        String fileSizeFormatted = FileUtils.formatFileSize(file.getFileSize());

        return FileContextDTO.builder()
                .fileId(String.valueOf(file.getId()))
                .achievementId(String.valueOf(file.getAchievementId()))
                .fileName(file.getFileName())
                .fileType(file.getFileType())
                .fileSize(file.getFileSize())
                .fileSizeFormatted(fileSizeFormatted)
                .fileUrl(fileUrl)
                .uploaderName(uploadUserName)
                .uploadAt(file.getUploadAt())
                .extension(file.getFileType())
                .content(null)
                .build();
    }

    /**
     * 批量获取文件上下文（用于 AI 对话）
     *
     * @param fileIds 文件 ID 列表
     * @return 文件上下文列表
     */
    @Override
    public List<FileContextDTO> getFileContexts(List<Long> fileIds) {
        log.info("[文件上下文批量] 获取文件信息: fileIds={}, count={}", fileIds, fileIds != null ? fileIds.size() : 0);

        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<FileContextDTO> contexts = new ArrayList<>();

        for (Long fileId : fileIds) {
            try {
                FileContextDTO context = getFileContext(fileId);
                if (context != null) {
                    contexts.add(context);
                }
            } catch (Exception e) {
                log.error("[文件上下文批量] 获取文件信息失败: fileId={}", fileId, e);
                // 继续处理其他文件，不中断整个流程
            }
        }

        log.info("[文件上下文批量] 成功获取 {} 个文件信息", contexts.size());
        return contexts;
    }
}
