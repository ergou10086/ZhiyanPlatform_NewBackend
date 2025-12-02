package hbnu.project.zhiyanbackend.wiki.service;

import hbnu.project.zhiyanbackend.wiki.model.dto.WikiAttachmentDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiAttachmentUploadDTO;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Wiki附件对象存储服务
 * 负责Wiki相关附件的上传、查询、下载、删除及统计分析等全生命周期管理
 * 支持单文件/多文件上传、分类查询（图片/普通文件）、临时下载链接生成、软硬删除等功能
 *
 * @author ErgouTree
 */
public interface WikiOssService {

    /**
     * 上传单个Wiki附件
     * 支持文件元数据记录、对象存储持久化，自动关联Wiki页面和项目
     *
     * @param file       待上传的文件
     * @param uploadDTO  上传参数DTO
     * @return WikiAttachmentDTO 上传成功后的附件信息DTO
     */
    WikiAttachmentDTO uploadAttachment(MultipartFile file, WikiAttachmentUploadDTO uploadDTO);

    /**
     * 批量上传Wiki附件
     * 内部复用单文件上传逻辑，支持批量处理和异常兼容
     *
     * @param files      待上传的文件数组
     * @param uploadDTO  上传参数DTO
     * @return List<WikiAttachmentDTO> 批量上传后的附件信息列表
     */
    List<WikiAttachmentDTO> uploadAttachments(MultipartFile[] files, WikiAttachmentUploadDTO uploadDTO);

    /**
     * 获取指定Wiki页面的所有附件
     * 按附件创建时间降序排序，返回该页面关联的全部类型附件（图片+普通文件）
     *
     * @param wikiPageId Wiki页面ID（关联附件所属的页面）
     * @return List<WikiAttachmentDTO> 页面附件列表（包含所有类型的附件完整信息）
     */
    List<WikiAttachmentDTO> getPageAttachments(Long wikiPageId);

    /**
     * 获取指定Wiki页面的图片类型附件
     * 筛选文件类型为图片（如jpg、png、gif等）的附件，适用于页面图片预览场景
     *
     * @param wikiPageId Wiki页面ID（关联附件所属的页面）
     * @return List<WikiAttachmentDTO> 图片附件列表（仅包含图片类型的附件）
     */
    List<WikiAttachmentDTO> getPageImages(Long wikiPageId);

    /**
     * 获取指定Wiki页面的普通文件类型附件
     * 筛选非图片类型的附件（如doc、pdf、zip等），适用于文件下载场景
     *
     * @param wikiPageId Wiki页面ID（关联附件所属的页面）
     * @return List<WikiAttachmentDTO> 普通文件附件列表（排除图片类型的附件）
     */
    List<WikiAttachmentDTO> getPageFiles(Long wikiPageId);

    /**
     * 根据附件ID获取单个附件详情
     * 包含附件的存储路径、文件大小、上传时间、关联页面等完整信息
     *
     * @param attachmentId 附件ID（唯一标识单个附件）
     * @return WikiAttachmentDTO 附件详情DTO（包含附件的全部元数据信息）
     */
    WikiAttachmentDTO getAttachment(Long attachmentId);

    /**
     * 生成附件临时下载URL（预签名URL）
     * 基于对象存储服务生成具有时效性的下载链接，避免附件直接暴露，保障访问安全
     *
     * @param attachmentId  附件ID（指定要生成下载链接的附件）
     * @param expireMinutes URL过期时间（单位：分钟，超过时间后链接失效）
     * @return String 临时下载URL（可直接用于前端下载，有效期内有效）
     */
    String generateDownloadUrl(Long attachmentId, Integer expireMinutes);

    /**
     * 软删除附件（逻辑删除）
     * 仅在数据库中标记附件为删除状态，不删除对象存储中的实际文件，支持数据恢复
     *
     * @param attachmentId 附件ID（要删除的附件）
     * @param operatorId   操作者ID（记录删除操作的用户）
     */
    void deleteAttachment(Long attachmentId, Long operatorId);

    /**
     * 物理删除附件
     * 同时删除对象存储中的实际文件和数据库中的附件记录，数据不可恢复，谨慎使用
     *
     * @param attachmentId 附件ID（要彻底删除的附件）
     */
    void deleteAttachmentPermanently(Long attachmentId);

    /**
     * 删除指定Wiki页面下的所有附件（物理删除）
     * 批量删除页面关联的所有附件，包括对象存储文件和数据库记录，适用于页面删除场景
     *
     * @param wikiPageId Wiki页面ID（要删除所有附件的页面）
     */
    void deletePageAttachments(Long wikiPageId);

    /**
     * 获取指定项目的附件统计数据
     * 包含附件总数、图片数量、普通文件数量、总存储大小、各类型文件占比等统计信息
     *
     * @param projectId 项目ID（指定要统计的项目）
     * @return Map<String, Object> 统计结果映射（key为统计项名称，value为对应统计值）
     */
    Map<String, Object> getProjectAttachmentStats(Long projectId);
}
