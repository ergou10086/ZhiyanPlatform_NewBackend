package hbnu.project.zhiyanbackend.wiki.service.impl;

import com.qcloud.cos.http.HttpMethodName;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.FileUtils;
import hbnu.project.zhiyanbackend.basic.utils.MimeTypeUtils;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import hbnu.project.zhiyanbackend.wiki.model.convert.WikiAttachmentMapper;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiAttachmentDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiAttachmentUploadDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiAttachment;
import hbnu.project.zhiyanbackend.wiki.model.enums.AttachmentType;
import hbnu.project.zhiyanbackend.wiki.repository.WikiAttachmentRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiOssService;
import hbnu.project.zhiyanbackend.oss.config.COSProperties;
import hbnu.project.zhiyanbackend.oss.dto.UploadFileResponseDTO;
import hbnu.project.zhiyanbackend.oss.service.COSService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static hbnu.project.zhiyanbackend.basic.utils.MimeTypeUtils.getExtension;

/**
 * Wiki附件对象存储服务实现（腾讯COS）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiOssServiceImpl implements WikiOssService {

    /**
     * 单附件限制100MB
     */
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    /**
     * 支持的图片格式
     */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");

    private final WikiAttachmentRepository attachmentRepository;

    private final COSService cosService;

    private final COSProperties cosProperties;

    private final WikiAttachmentMapper wikiAttachmentMapper;

    // ========== 上传 ==========

    /**
     * 上传单个Wiki附件
     *
     * @param file       待上传的文件
     * @param uploadDTO  上传参数DTO
     * @return WikiAttachmentDTO 附件上传dto
     */
    @Override
    @Transactional
    public WikiAttachmentDTO uploadAttachment(MultipartFile file, WikiAttachmentUploadDTO uploadDTO) {
        validateUploadArgs(file, uploadDTO);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String mimeType = resolveMimeType(file, extension);

        AttachmentType attachmentType = resolveAttachmentType(extension, uploadDTO.getAttachmentType());

        String businessFolder = buildBusinessFolder(uploadDTO.getProjectId(), uploadDTO.getWikiPageId());
        UploadFileResponseDTO uploadResult = cosService.uploadFileSenior(file, businessFolder, uploadDTO.getCustomFileName());

        String fileHash = calculateFileHash(file);

        WikiAttachment attachment = WikiAttachment.builder()
                .wikiPageId(uploadDTO.getWikiPageId())
                .projectId(uploadDTO.getProjectId())
                .attachmentType(attachmentType)
                .fileName(originalFilename)
                .fileSize(file.getSize())
                .fileType(extension)
                .mimeType(mimeType)
                .bucketName(cosProperties.getBucketName())
                .objectKey(uploadResult.getObjectKey())
                .fileUrl(uploadResult.getUrl())
                .description(uploadDTO.getDescription())
                .fileHash(fileHash)
                .uploadBy(uploadDTO.getUploadBy())
                .metadata(buildMetadataMap(file, mimeType))
                .build();

        attachment = attachmentRepository.save(attachment);

        log.info("Wiki附件上传成功: attachmentId={}, projectId={}, wikiPageId={}, size={}",
                attachment.getId(), uploadDTO.getProjectId(), uploadDTO.getWikiPageId(), file.getSize());

        return wikiAttachmentMapper.toDTO(attachment);
    }

    /**
     * 批量上传Wiki附件
     * 内部复用单文件上传逻辑，支持批量处理和异常兼容
     *
     * @param files      待上传的文件数组
     * @param uploadDTO  上传参数DTO
     * @return WikiAttachmentDTO的列表
     */
    @Override
    @Transactional
    public List<WikiAttachmentDTO> uploadAttachments(MultipartFile[] files, WikiAttachmentUploadDTO uploadDTO) {
        ValidationUtils.requireNonNull(files, "文件列表不能为空");

        List<WikiAttachmentDTO> results = new ArrayList<>();
        for(MultipartFile file : files){
            try {
                results.add(uploadAttachment(file, uploadDTO));
            } catch (Exception e) {
                log.error("批量上传某文件失败: fileName={}", file.getOriginalFilename(), e);
            }
        }

        return results;
    }

    // ========== 查询 ==========

    /**
     * 获取指定Wiki页面的所有附件
     *
     * @param wikiPageId Wiki页面ID（关联附件所属的页面）
     * @return WikiAttachmentDTO的列表
     */
    @Override
    public List<WikiAttachmentDTO> getPageAttachments(Long wikiPageId) {
        return attachmentRepository.findByWikiPageIdAndIsDeletedFalse(wikiPageId)
                .stream()
                .map(wikiAttachmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定Wiki页面的图片类型附件
     *
     * @param wikiPageId Wiki页面ID（关联附件所属的页面）
     * @return WikiAttachmentDTO的列表
     */
    @Override
    public List<WikiAttachmentDTO> getPageImages(Long wikiPageId) {
        return attachmentRepository.findByWikiPageIdAndAttachmentTypeAndIsDeletedFalse(wikiPageId, AttachmentType.IMAGE)
                .stream()
                .map(wikiAttachmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定Wiki页面的普通文件类型附件
     *
     * @param wikiPageId Wiki页面ID（关联附件所属的页面）
     * @return WikiAttachmentDTO的列表
     */
    @Override
    public List<WikiAttachmentDTO> getPageFiles(Long wikiPageId) {
        return attachmentRepository.findByWikiPageIdAndAttachmentTypeAndIsDeletedFalse(wikiPageId, AttachmentType.FILE)
                .stream()
                .map(wikiAttachmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据附件ID获取单个附件详情
     * 包含附件的存储路径、文件大小、上传时间、关联页面等完整信息
     *
     * @param attachmentId 附件ID（唯一标识单个附件）
     * @return WikiAttachmentDTO 附件详情DTO（包含附件的全部元数据信息）
     */
    @Override
    public WikiAttachmentDTO getAttachment(Long attachmentId) {
        WikiAttachment attachment = attachmentRepository.findByIdAndIsDeletedFalse(attachmentId)
                .orElseThrow(() -> new ServiceException("附件不存在或已被删除"));
        return wikiAttachmentMapper.toDTO(attachment);
    }

    /**
     * 生成附件临时下载URL（预签名URL）
     * 基于对象存储服务生成具有时效性的下载链接，避免附件直接暴露，保障访问安全
     *
     * @param attachmentId  附件ID（指定要生成下载链接的附件）
     * @param expireMinutes URL过期时间（单位：分钟，超过时间后链接失效）
     * @return String 临时下载URL（可直接用于前端下载，有效期内有效）
     */
    @Override
    public String generateDownloadUrl(Long attachmentId, Integer expireMinutes) {
        WikiAttachment attachment = attachmentRepository.findByIdAndIsDeletedFalse(attachmentId)
                .orElseThrow(() -> new ServiceException("附件不存在或已被删除"));

        // 一小时过期
        int minutes = (expireMinutes != null && expireMinutes > 0) ? expireMinutes : 60;
        Date expiration = new Date(System.currentTimeMillis() + minutes * 60L * 1000L);

        URL url = cosService.generatePresignedUrl(
                attachment.getBucketName(),
                attachment.getObjectKey(),
                expiration,
                HttpMethodName.GET,
                null,
                null,
                false,
                true
        );

        return url.toString();
    }

    // ========== 删除 ==========

    /**
     * 软删除附件（逻辑删除）
     * 仅在数据库中标记附件为删除状态，不删除对象存储中的实际文件，支持数据恢复
     *
     * @param attachmentId 附件ID（要删除的附件）
     * @param operatorId   操作者ID（记录删除操作的用户）
     */
    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, Long operatorId) {
        int rows = attachmentRepository.softDeleteById(attachmentId, operatorId);
        if (rows == 0) {
            throw new ServiceException("附件不存在或已被删除");
        }
        log.info("附件软删除成功: attachmentId={}, operator={}", attachmentId, operatorId);
    }

    /**
     * 物理删除附件
     * 同时删除对象存储中的实际文件和数据库中的附件记录，数据不可恢复，谨慎使用
     *
     * @param attachmentId 附件ID（要彻底删除的附件）
     */
    @Override
    @Transactional
    public void deleteAttachmentPermanently(Long attachmentId) {
        WikiAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ServiceException("附件不存在"));

        cosService.deleteObject(attachment.getBucketName(), attachment.getObjectKey());
        attachmentRepository.delete(attachment);

        log.info("附件物理删除成功: attachmentId={}, key={}", attachmentId, attachment.getObjectKey());
    }

    /**
     * 删除指定Wiki页面下的所有附件（物理删除）
     * 批量删除页面关联的所有附件，包括对象存储文件和数据库记录，适用于页面删除场景
     *
     * @param wikiPageId Wiki页面ID（要删除所有附件的页面）
     */
    @Override
    @Transactional
    public void deletePageAttachments(Long wikiPageId) {
        List<WikiAttachment> attachments = attachmentRepository.findByWikiPageIdAndIsDeletedFalse(wikiPageId);
        for (WikiAttachment attachment : attachments) {
            try {
                cosService.deleteObject(attachment.getBucketName(), attachment.getObjectKey());
                attachmentRepository.delete(attachment);
            } catch (Exception e) {
                log.error("删除附件失败: attachmentId={}", attachment.getId(), e);
            }
        }
        log.info("删除页面附件完成: wikiPageId={}, count={}", wikiPageId, attachments.size());
    }

    // ========== 统计 ==========

    /**
     * 获取指定项目的附件统计数据
     * 包含附件总数、图片数量、普通文件数量、总存储大小、各类型文件占比等统计信息
     *
     * @param projectId 项目ID（指定要统计的项目）
     * @return Map<String, Object> 统计结果映射（key为统计项名称，value为对应统计值）
     */
    @Override
    public Map<String, Object> getProjectAttachmentStats(Long projectId) {
        Map<String, Object> stats = new HashMap<>();

        long totalCount = attachmentRepository.countByProjectIdAndIsDeletedFalse(projectId);
        long imageCount = attachmentRepository.countByProjectIdAndAttachmentTypeAndIsDeletedFalse(projectId, AttachmentType.IMAGE);
        long fileCount = attachmentRepository.countByProjectIdAndAttachmentTypeAndIsDeletedFalse(projectId, AttachmentType.FILE);
        long totalSize = Optional.ofNullable(attachmentRepository.sumFileSizeByProjectId(projectId)).orElse(0L);

        stats.put("totalCount", totalCount);
        stats.put("imageCount", imageCount);
        stats.put("fileCount", fileCount);
        stats.put("totalSize", totalSize);
        stats.put("totalSizeFormatted", FileUtils.formatFileSize(totalSize));

        Map<String, Long> typeDistribution = attachmentRepository.countByAttachmentType(projectId).stream()
                .collect(Collectors.toMap(
                        row -> ((AttachmentType) row[0]).name(),
                        row -> ((Number) row[1]).longValue()
                ));
        stats.put("typeDistribution", typeDistribution);

        Map<String, Long> extensionDistribution = attachmentRepository.countByFileType(projectId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
        stats.put("extensionDistribution", extensionDistribution);

        return stats;
    }

    // ========== 辅助方法 ==========
    private void validateUploadArgs(MultipartFile file, WikiAttachmentUploadDTO uploadDTO) {
        ValidationUtils.requireNonNull(file, "文件不能为空");
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceException("文件大小超过限制（最大100MB）");
        }
        if (uploadDTO == null
                || uploadDTO.getProjectId() == null
                || uploadDTO.getWikiPageId() == null
                || uploadDTO.getUploadBy() == null) {
            throw new ServiceException("上传参数不完整");
        }
    }

    // 构建对象键，带文件夹
    private String buildBusinessFolder(Long projectId, Long wikiPageId) {
        return String.format("wiki/%d/%d", projectId, wikiPageId);
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        String ext = StringUtils.getFilenameExtension(filename);
        return ext != null ? ext.toLowerCase() : "";
    }

    /**
     * 确认mime类型
     */
    private String resolveMimeType(MultipartFile file, String extension) {
        String mimeType = file.getContentType();
        if (!StringUtils.hasText(mimeType)) {
            mimeType = MimeTypeUtils.getExtension(extension);
        }
        return StringUtils.hasText(mimeType) ? mimeType : "application/octet-stream";
    }

    /**
     * 识别附件类型
     */
    private AttachmentType resolveAttachmentType(String extension, String specifiedType) {
        if (StringUtils.hasText(specifiedType)) {
            try {
                return AttachmentType.valueOf(specifiedType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("非法附件类型: {}, 将自动识别", specifiedType);
            }
        }
        return IMAGE_EXTENSIONS.contains(extension) ? AttachmentType.IMAGE : AttachmentType.FILE;
    }

    private String calculateFileHash(MultipartFile file) {
        try {
            return DigestUtils.md5DigestAsHex(file.getInputStream());
        } catch (IOException e) {
            log.warn("计算文件MD5失败: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildMetadataMap(MultipartFile file, String mimeType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", mimeType);
        metadata.put("size", file.getSize());
        return metadata;
    }
}
