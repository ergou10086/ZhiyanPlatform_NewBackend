package hbnu.project.zhiyanbackend.knowledge.service;

import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.UploadFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.FileContextDTO;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 成果文件服务接口
 * 提供成果文件的上传、下载、删除等功能
 *
 * @author ErgouTree
 */
public interface AchievementFileService {

    /**
     * 上传成果文件
     *
     * @param file      文件
     * @param uploadDTO 上传DTO
     * @return 文件信息
     */
    AchievementFileDTO uploadFile(MultipartFile file, UploadFileDTO uploadDTO);

    /**
     * 批量上传成果文件
     *
     * @param files        文件列表
     * @param achievementId 成果ID
     * @param uploadBy      上传者ID
     * @return 文件信息列表
     */
    List<AchievementFileDTO> uploadFilesBatch(MultipartFile[] files, Long achievementId, Long uploadBy);

    /**
     * 获取文件预签名URL
     *
     * @param fileId       文件ID
     * @param userId       当前用户ID
     * @param expirySeconds 过期时间（秒），默认3天
     * @return 预签名URL
     */
    String generatePresignedUrl(Long fileId, Long userId, Integer expirySeconds);

    /**
     * 下载成果文件（通过预签名URL重定向）
     *
     * @param fileId 文件ID
     * @param userId 当前用户ID（用于权限验证）
     */
    void downloadFile(Long fileId, Long userId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param userId 当前用户ID（用于权限验证）
     */
    void deleteFile(Long fileId, Long userId);

    /**
     * 根据成果ID批量删除文件
     * 用于删除整个成果时调用
     *
     * @param achievementId 成果ID
     * @param userId 操作者ID
     */
    void deleteFilesByAchievementId(Long achievementId, Long userId);

    /**
     * 批量删除文件
     *
     * @param fileIds 文件ID列表
     * @param userId  当前用户ID
     */
    void deleteFiles(List<Long> fileIds, Long userId);

    /**
     * 查询成果的所有文件
     *
     * @param achievementId 成果ID
     * @return 文件列表
     */
    List<AchievementFileDTO> getFilesByAchievementId(Long achievementId);

    /**
     * 根据ID获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件DTO
     */
    AchievementFileDTO getFileById(Long fileId);

    /**
     * 统计成果的文件数量
     *
     * @param achievementId 成果ID
     * @return 文件数量
     */
    long countFilesByAchievementId(Long achievementId);

    /**
     * 检查用户是否有权限访问文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean hasFilePermission(Long fileId, Long userId);

    /**
     * 获取文件上下文（用于 AI 对话）
     *
     * @param fileId 文件 ID
     * @return 文件上下文信息
     */
    FileContextDTO getFileContext(Long fileId);

    /**
     * 批量获取文件上下文（用于 AI 对话）
     *
     * @param fileIds 文件 ID 列表
     * @return 文件上下文列表
     */
    List<FileContextDTO> getFileContexts(List<Long> fileIds);
}
