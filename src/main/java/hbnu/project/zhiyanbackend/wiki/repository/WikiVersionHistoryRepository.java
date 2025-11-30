package hbnu.project.zhiyanbackend.wiki.repository;

import hbnu.project.zhiyanbackend.wiki.model.entity.WikiVersionHistory;
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
 * Wiki版本历史记录数据访问层
 * 用于查询和管理归档的历史版本（从wiki_page.recent_versions归档而来）
 * 支持PostgreSQL分区表特性
 *
 * @author ErgouTree
 */
@Repository
public interface WikiVersionHistoryRepository {

    // ==================== 基础查询 ====================

    /**
     * 根据Wiki页面ID和版本号查询特定历史版本
     *
     * @param wikiPageId Wiki页面ID
     * @param version    版本号
     * @return 历史版本
     */
    Optional<WikiVersionHistory> findByWikiPageIdAndVersion(Long wikiPageId, Integer version);

    /**
     * 根据Wiki页面ID查询所有历史版本
     * 按版本号降序
     *
     * @param wikiPageId Wiki页面ID
     * @return 历史版本列表
     */
    List<WikiVersionHistory> findByWikiPageIdOrderByVersionDesc(Long wikiPageId);

    /**
     * 根据Wiki页面ID分页查询历史版本
     *
     * @param wikiPageId Wiki页面ID
     * @param pageable   分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByWikiPageIdOrderByVersionDesc(Long wikiPageId, Pageable pageable);

    /**
     * 根据Wiki页面ID查询历史版本（按创建时间降序）
     *
     * @param wikiPageId Wiki页面ID
     * @param pageable   分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByWikiPageIdOrderByCreatedAtDesc(Long wikiPageId, Pageable pageable);

    // ==================== 版本范围查询 ====================

    /**
     * 根据Wiki页面ID查询版本号范围内的历史版本
     *
     * @param wikiPageId Wiki页面ID
     * @param minVersion 最小版本号（包含）
     * @param maxVersion 最大版本号（包含）
     * @return 历史版本列表（按版本号升序）
     */
    @Query("SELECT v FROM WikiVersionHistory v " +
            "WHERE v.wikiPageId = :wikiPageId AND v.version BETWEEN :minVersion AND :maxVersion " +
            "ORDER BY v.version ASC")
    List<WikiVersionHistory> findByWikiPageIdAndVersionBetween(
            @Param("wikiPageId") Long wikiPageId,
            @Param("minVersion") Integer minVersion,
            @Param("maxVersion") Integer maxVersion);

    /**
     * 查询Wiki页面指定版本之后的所有版本
     *
     * @param wikiPageId   Wiki页面ID
     * @param afterVersion 版本号
     * @param pageable     分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByWikiPageIdAndVersionGreaterThanOrderByVersionAsc(
            Long wikiPageId, Integer afterVersion, Pageable pageable);

    /**
     * 查询Wiki页面指定版本之前的所有版本
     *
     * @param wikiPageId    Wiki页面ID
     * @param beforeVersion 版本号
     * @param pageable      分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByWikiPageIdAndVersionLessThanOrderByVersionDesc(
            Long wikiPageId, Integer beforeVersion, Pageable pageable);

    // ==================== 项目相关查询 ====================

    /**
     * 根据项目ID查询所有历史版本（按创建时间降序）
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    /**
     * 根据项目ID查询所有历史版本（按归档时间降序）
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByProjectIdOrderByArchivedAtDesc(Long projectId, Pageable pageable);

    /**
     * 统计项目的历史版本总数
     *
     * @param projectId 项目ID
     * @return 版本数量
     */
    long countByProjectId(Long projectId);

    // ==================== 用户相关查询 ====================

    /**
     * 根据编辑者ID查询历史版本（使用createdBy字段）
     *
     * @param editorId 编辑者ID
     * @param pageable 分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByCreatedByOrderByCreatedAtDesc(Long editorId, Pageable pageable);

    /**
     * 根据项目ID和编辑者ID查询历史版本
     *
     * @param projectId 项目ID
     * @param editorId  编辑者ID
     * @param pageable  分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByProjectIdAndCreatedByOrderByCreatedAtDesc(
            Long projectId, Long editorId, Pageable pageable);

    /**
     * 统计用户的编辑次数
     *
     * @param editorId 编辑者ID
     * @return 编辑次数
     */
    long countByCreatedBy(Long editorId);

    // ==================== 统计查询 ====================

    /**
     * 统计Wiki页面的历史版本数量
     *
     * @param wikiPageId Wiki页面ID
     * @return 历史版本数量
     */
    long countByWikiPageId(Long wikiPageId);

    /**
     * 查询Wiki页面的最大版本号
     *
     * @param wikiPageId Wiki页面ID
     * @return 最大版本号
     */
    @Query("SELECT MAX(v.version) FROM WikiVersionHistory v WHERE v.wikiPageId = :wikiPageId")
    Optional<Integer> findMaxVersionByWikiPageId(@Param("wikiPageId") Long wikiPageId);

    /**
     * 查询Wiki页面的最小版本号
     *
     * @param wikiPageId Wiki页面ID
     * @return 最小版本号
     */
    @Query("SELECT MIN(v.version) FROM WikiVersionHistory v WHERE v.wikiPageId = :wikiPageId")
    Optional<Integer> findMinVersionByWikiPageId(@Param("wikiPageId") Long wikiPageId);

    /**
     * 统计时间范围内的版本数量
     *
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 版本数量
     */
    long countByProjectIdAndCreatedAtBetween(Long projectId, LocalDateTime startTime, LocalDateTime endTime);

    // ==================== 变更统计 ====================

    /**
     * 查询最活跃的编辑者（按版本数量）
     *
     * @param projectId 项目ID
     * @return 编辑者ID和版本数列表 [editorId, count]
     */
    @Query("SELECT v.createdBy, COUNT(v) as cnt FROM WikiVersionHistory v " +
            "WHERE v.projectId = :projectId " +
            "GROUP BY v.createdBy " +
            "ORDER BY cnt DESC")
    List<Object[]> findTopEditors(@Param("projectId") Long projectId, Pageable pageable);

    /**
     * 统计Wiki页面的总变更量
     *
     * @param wikiPageId Wiki页面ID
     * @return 统计结果 [totalAddedLines, totalDeletedLines, totalChangedChars]
     */
    @Query("SELECT COALESCE(SUM(v.addedLines), 0), COALESCE(SUM(v.deletedLines), 0), " +
            "COALESCE(SUM(v.changedChars), 0) FROM WikiVersionHistory v " +
            "WHERE v.wikiPageId = :wikiPageId")
    Object[] sumChangeStatsByWikiPageId(@Param("wikiPageId") Long wikiPageId);

    /**
     * 按日期统计版本数量
     *
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计结果 [date, count]
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
            "FROM zhiyanwiki.wiki_version_history " +
            "WHERE project_id = :projectId " +
            "AND created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(created_at) ORDER BY date",
            nativeQuery = true)
    List<Object[]> countByDateRange(@Param("projectId") Long projectId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // ==================== 时间范围查询 ====================

    /**
     * 查询指定时间之前创建的历史版本（用于清理旧数据）
     *
     * @param createdAt 截止时间
     * @return 历史版本列表
     */
    List<WikiVersionHistory> findByCreatedAtBefore(LocalDateTime createdAt);

    /**
     * 查询指定时间之前归档的历史版本
     *
     * @param archivedAt 截止时间
     * @return 历史版本列表
     */
    List<WikiVersionHistory> findByArchivedAtBefore(LocalDateTime archivedAt);

    /**
     * 查询时间范围内的历史版本
     *
     * @param wikiPageId Wiki页面ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param pageable   分页参数
     * @return 历史版本分页列表
     */
    Page<WikiVersionHistory> findByWikiPageIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long wikiPageId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ==================== 内容哈希查询 ====================

    /**
     * 根据内容哈希查询版本（用于检测重复内容）
     *
     * @param wikiPageId  Wiki页面ID
     * @param contentHash 内容哈希值
     * @return 历史版本列表
     */
    List<WikiVersionHistory> findByWikiPageIdAndContentHash(Long wikiPageId, String contentHash);

    /**
     * 检查是否存在指定哈希的版本
     *
     * @param wikiPageId  Wiki页面ID
     * @param contentHash 内容哈希值
     * @return 是否存在
     */
    boolean existsByWikiPageIdAndContentHash(Long wikiPageId, String contentHash);

    // ==================== 批量删除 ====================

    /**
     * 删除Wiki页面的所有历史版本
     *
     * @param wikiPageId Wiki页面ID
     */
    void deleteByWikiPageId(Long wikiPageId);

    /**
     * 删除项目下的所有历史版本
     *
     * @param projectId 项目ID
     */
    void deleteByProjectId(Long projectId);

    /**
     * 删除指定时间之前的历史版本（用于数据清理）
     *
     * @param createdAt 截止时间
     * @return 删除数量
     */
    @Modifying
    @Query("DELETE FROM WikiVersionHistory v WHERE v.createdAt < :createdAt")
    int deleteByCreatedAtBefore(@Param("createdAt") LocalDateTime createdAt);

    /**
     * 删除Wiki页面指定版本之前的所有版本
     *
     * @param wikiPageId    Wiki页面ID
     * @param beforeVersion 版本号
     * @return 删除数量
     */
    @Modifying
    @Query("DELETE FROM WikiVersionHistory v " +
            "WHERE v.wikiPageId = :wikiPageId AND v.version < :beforeVersion")
    int deleteByWikiPageIdAndVersionBefore(@Param("wikiPageId") Long wikiPageId,
                                           @Param("beforeVersion") Integer beforeVersion);

    /**
     * 批量删除指定版本范围的历史记录
     *
     * @param wikiPageId Wiki页面ID
     * @param minVersion 最小版本号
     * @param maxVersion 最大版本号
     * @return 删除数量
     */
    @Modifying
    @Query("DELETE FROM WikiVersionHistory v " +
            "WHERE v.wikiPageId = :wikiPageId AND v.version BETWEEN :minVersion AND :maxVersion")
    int deleteByWikiPageIdAndVersionBetween(@Param("wikiPageId") Long wikiPageId,
                                            @Param("minVersion") Integer minVersion,
                                            @Param("maxVersion") Integer maxVersion);

    // ==================== 最近版本查询 ====================

    /**
     * 查询Wiki页面最近的N个版本
     *
     * @param wikiPageId Wiki页面ID
     * @param pageable   分页参数
     * @return 历史版本列表
     */
    @Query("SELECT v FROM WikiVersionHistory v " +
            "WHERE v.wikiPageId = :wikiPageId " +
            "ORDER BY v.version DESC")
    List<WikiVersionHistory> findRecentVersions(@Param("wikiPageId") Long wikiPageId, Pageable pageable);

    /**
     * 查询项目最近的版本历史
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return 历史版本列表
     */
    @Query("SELECT v FROM WikiVersionHistory v " +
            "WHERE v.projectId = :projectId " +
            "ORDER BY v.createdAt DESC")
    List<WikiVersionHistory> findRecentByProject(@Param("projectId") Long projectId, Pageable pageable);

    // ==================== 版本比较辅助 ====================

    /**
     * 查询两个版本之间的所有版本（用于diff比较）
     *
     * @param wikiPageId   Wiki页面ID
     * @param fromVersion  起始版本（不包含）
     * @param toVersion    结束版本（包含）
     * @return 历史版本列表
     */
    @Query("SELECT v FROM WikiVersionHistory v " +
            "WHERE v.wikiPageId = :wikiPageId " +
            "AND v.version > :fromVersion AND v.version <= :toVersion " +
            "ORDER BY v.version ASC")
    List<WikiVersionHistory> findVersionsForDiff(@Param("wikiPageId") Long wikiPageId,
                                                 @Param("fromVersion") Integer fromVersion,
                                                 @Param("toVersion") Integer toVersion);

    // ==================== 存储优化 ====================

    /**
     * 查询大版本记录（用于优化存储）
     * 查找差异补丁大小超过阈值的版本
     *
     * @param threshold 字符数阈值
     * @return 版本列表
     */
    @Query(value = "SELECT * FROM zhiyanwiki.wiki_version_history " +
            "WHERE LENGTH(content_diff) > :threshold " +
            "ORDER BY LENGTH(content_diff) DESC",
            nativeQuery = true)
    List<WikiVersionHistory> findLargeVersions(@Param("threshold") int threshold, Pageable pageable);

    /**
     * 统计项目的版本存储总大小（估算）
     *
     * @param projectId 项目ID
     * @return 总字符数
     */
    @Query(value = "SELECT COALESCE(SUM(LENGTH(content_diff)), 0) " +
            "FROM zhiyanwiki.wiki_version_history WHERE project_id = :projectId",
            nativeQuery = true)
    Long sumStorageSizeByProjectId(@Param("projectId") Long projectId);
}
