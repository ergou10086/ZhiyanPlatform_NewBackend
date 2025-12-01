package hbnu.project.zhiyanbackend.wiki.repository;

import hbnu.project.zhiyanbackend.wiki.model.entity.WikiAttachment;
import hbnu.project.zhiyanbackend.wiki.model.enums.AttachmentType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Wiki附件Repository
 * 提供附件的数据访问操作
 * 支持软删除、引用计数、文件去重等功能
 *
 * @author Tokito
 */
@Repository
public interface WikiAttachmentRepository extends JpaRepository<WikiAttachment, Long> {

    // ==================== 基础查询 ====================

    /**
     * 根据ID查询附件（不包括已删除）
     *
     * @param id 附件ID
     * @return 附件Optional
     */
    Optional<WikiAttachment> findByIdAndIsDeletedFalse(Long id);

    /**
     * 根据对象键查询附件（用于文件上传去重）
     *
     * @param objectKey 对象键
     * @return 附件Optional
     */
    Optional<WikiAttachment> findByObjectKeyAndIsDeletedFalse(String objectKey);

    /**
     * 根据文件哈希查询附件（用于文件去重）
     *
     * @param fileHash 文件哈希值
     * @return 附件列表
     */
    List<WikiAttachment> findByFileHashAndIsDeletedFalse(String fileHash);

    // ==================== Wiki页面相关查询 ====================

    /**
     * 根据Wiki页面查询所有附件
     * 不包括已删除
     *
     * @param wikiPageId Wiki页面ID
     * @return 附件列表
     */
    List<WikiAttachment> findByWikiPageIdAndIsDeletedFalse(Long wikiPageId);

    /**
     * 根据Wiki页面ID和附件类型查询附件
     *
     * @param wikiPageId     Wiki页面ID
     * @param attachmentType 附件类型
     * @return 附件列表
     */
    List<WikiAttachment> findByWikiPageIdAndAttachmentTypeAndIsDeletedFalse(
            Long wikiPageId, AttachmentType attachmentType);

    /**
     * 统计Wiki页面指定类型的附件数量
     *
     * @param wikiPageId     Wiki页面ID
     * @param attachmentType 附件类型
     * @return 附件数量
     */
    long countByWikiPageIdAndAttachmentTypeAndIsDeletedFalse(Long wikiPageId, AttachmentType attachmentType);

    // ==================== 项目相关查询 ====================

    /**
     * 根据项目ID查询所有附件（分页）
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndIsDeletedFalse(Long projectId, Pageable pageable);

    /**
     * 根据项目ID和附件类型查询附件（分页）
     *
     * @param projectId      项目ID
     * @param attachmentType 附件类型
     * @param pageable       分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndAttachmentTypeAndIsDeletedFalse(
            Long projectId, AttachmentType attachmentType, Pageable pageable);

    /**
     * 统计项目的附件总数
     *
     * @param projectId 项目ID
     * @return 附件数量
     */
    long countByProjectIdAndIsDeletedFalse(Long projectId);

    /**
     * 统计项目指定类型的附件数量
     *
     * @param projectId      项目ID
     * @param attachmentType 附件类型
     * @return 附件数量
     */
    long countByProjectIdAndAttachmentTypeAndIsDeletedFalse(Long projectId, AttachmentType attachmentType);

    /**
     * 统计项目的附件总大小
     *
     * @param projectId 项目ID
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM WikiAttachment a " +
            "WHERE a.projectId = :projectId AND a.isDeleted = false ")
    Long sumFileSizeByProjectId(@Param("projectId") Long projectId);

    /**
     * 统计项目指定类型附件的总大小
     *
     * @param projectId      项目ID
     * @param attachmentType 附件类型
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM WikiAttachment a " +
            "WHERE a.projectId = :projectId AND a.attachmentType = :type AND a.isDeleted = false")
    Long sumFileSizeByProjectIdAndType(@Param("projectId") Long projectId,
                                       @Param("type") AttachmentType attachmentType);

    // ==================== 文件搜索 ====================

    /**
     * 根据文件名模糊查询附件
     *
     * @param projectId 项目ID
     * @param fileName  文件名关键字
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndFileNameContainingIgnoreCaseAndIsDeletedFalse(
            Long projectId, String fileName, Pageable pageable);

    /**
     * 根据文件类型查询附件
     *
     * @param projectId 项目ID
     * @param fileType  文件类型（扩展名）
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndFileTypeAndIsDeletedFalse(Long projectId, String fileType, Pageable pageable);

    /**
     * 根据MIME类型查询附件
     *
     * @param projectId 项目ID
     * @param mimeType  MIME类型
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndMimeTypeAndIsDeletedFalse(Long projectId, String mimeType, Pageable pageable);

    // ==================== 用户相关查询 ====================

    /**
     * 根据上传者ID查询附件
     *
     * @param uploadBy 上传者ID
     * @param pageable 分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByUploadByAndIsDeletedFalse(Long uploadBy, Pageable pageable);

    /**
     * 根据项目ID和上传者ID查询附件
     *
     * @param projectId 项目ID
     * @param uploadBy  上传者ID
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndUploadByAndIsDeletedFalse(
            Long projectId, Long uploadBy, Pageable pageable);

    /**
     * 统计用户上传的附件数量
     *
     * @param uploadBy 上传者ID
     * @return 附件数量
     */
    long countByUploadByAndIsDeletedFalse(Long uploadBy);

    // ==================== 引用计数相关 ====================

    /**
     * 查询未被引用的附件（引用计数为0）
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndReferenceCountAndIsDeletedFalse(
            Long projectId, Integer referenceCount, Pageable pageable);

    /**
     * 增加附件的引用计数
     *
     * @param id 附件ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE WikiAttachment a SET a.referenceCount = a.referenceCount + 1 " +
            "WHERE a.id = :id")
    int incrementReferenceCount(@Param("id") Long id);

    /**
     * 减少附件的引用计数
     *
     * @param id 附件ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE WikiAttachment a SET a.referenceCount = a.referenceCount - 1 " +
            "WHERE a.id = :id AND a.referenceCount > 0")
    int decrementReferenceCount(@Param("id") Long id);

    /**
     * 查询引用计数为0且已删除的附件（可物理删除）
     *
     * @param deletedBefore 删除时间早于此时间
     * @return 附件列表
     */
    @Query("SELECT a FROM WikiAttachment a " +
            "WHERE a.referenceCount = 0 AND a.isDeleted = true " +
            "AND a.deletedAt < :deletedBefore")
    List<WikiAttachment> findDeletableAttachments(@Param("deletedBefore") LocalDateTime deletedBefore);

    // ==================== 软删除操作 ====================

    /**
     * 软删除附件
     *
     * @param id        附件ID
     * @param deletedBy 删除者ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE WikiAttachment a SET a.isDeleted = true, a.deletedAt = CURRENT_TIMESTAMP, " +
            "a.deletedBy = :deletedBy WHERE a.id = :id AND a.isDeleted = false")
    int softDeleteById(@Param("id") Long id, @Param("deletedBy") Long deletedBy);

    /**
     * 批量软删除Wiki页面的附件
     *
     * @param wikiPageId Wiki页面ID
     * @param deletedBy  删除者ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE WikiAttachment a SET a.isDeleted = true, a.deletedAt = CURRENT_TIMESTAMP, " +
            "a.deletedBy = :deletedBy WHERE a.wikiPageId = :wikiPageId AND a.isDeleted = false")
    int softDeleteByWikiPageId(@Param("wikiPageId") Long wikiPageId, @Param("deletedBy") Long deletedBy);

    /**
     * 批量软删除项目的所有附件
     *
     * @param projectId 项目ID
     * @param deletedBy 删除者ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE WikiAttachment a SET a.isDeleted = true, a.deletedAt = CURRENT_TIMESTAMP, " +
            "a.deletedBy = :deletedBy WHERE a.projectId = :projectId AND a.isDeleted = false")
    int softDeleteByProjectId(@Param("projectId") Long projectId, @Param("deletedBy") Long deletedBy);

    /**
     * 恢复软删除的附件
     *
     * @param id 附件ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE WikiAttachment a SET a.isDeleted = false, a.deletedAt = NULL, " +
            "a.deletedBy = NULL WHERE a.id = :id AND a.isDeleted = true")
    int restoreById(@Param("id") Long id);

    // ==================== 已删除附件查询（用于回收站） ====================

    /**
     * 查询项目的已删除附件
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByProjectIdAndIsDeletedTrue(Long projectId, Pageable pageable);

    /**
     * 查询用户删除的附件
     *
     * @param deletedBy 删除者ID
     * @param pageable  分页参数
     * @return 附件分页
     */
    Page<WikiAttachment> findByDeletedByAndIsDeletedTrueOrderByDeletedAtDesc(Long deletedBy, Pageable pageable);

    // ==================== 批量操作 ====================

    /**
     * 批量删除项目下的所有附件（物理删除）
     *
     * @param projectId 项目ID
     */
    void deleteByProjectId(Long projectId);

    /**
     * 批量删除Wiki页面的所有附件（物理删除）
     *
     * @param wikiPageId Wiki页面ID
     */
    void deleteByWikiPageId(Long wikiPageId);

    // ==================== 统计分析 ====================

    /**
     * 按文件类型统计附件数量
     *
     * @param projectId 项目ID
     * @return 统计结果 [fileType, count]
     */
    @Query("SELECT a.fileType, COUNT(a) FROM WikiAttachment a " +
            "WHERE a.projectId = :projectId AND a.isDeleted = false " +
            "GROUP BY a.fileType ORDER BY COUNT(a) DESC")
    List<Object[]> countByFileType(@Param("projectId") Long projectId);

    /**
     * 按附件类型统计附件数量
     *
     * @param projectId 项目ID
     * @return 统计结果 [attachmentType, count]
     */
    @Query("SELECT a.attachmentType, COUNT(a) FROM WikiAttachment a " +
            "WHERE a.projectId = :projectId AND a.isDeleted = false " +
            "GROUP BY a.attachmentType")
    List<Object[]> countByAttachmentType(@Param("projectId") Long projectId);
}
