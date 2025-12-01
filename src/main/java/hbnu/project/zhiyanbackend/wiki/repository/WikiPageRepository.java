package hbnu.project.zhiyanbackend.wiki.repository;

import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Wiki页面数据访问层
 * 提供Wiki页面的CRUD和树状结构查询
 * 支持PostgreSQL特性：全文搜索、ltree路径查询等
 *
 * @author ErgouTree
 * @rewrite ErgouTree
 */
@Repository
public interface WikiPageRepository extends JpaRepository<WikiPage, Long> {

    // ==================== 基础查询 ====================

    /**
     * 根据ID和项目ID查询Wiki页面（用于权限校验）
     *
     * @param id        页面ID
     * @param projectId 项目ID
     * @return Wiki页面
     */
    Optional<WikiPage> findByIdAndProjectId(Long id, Long projectId);

    /**
     * 根据项目ID查询所有Wiki页面
     *
     * @param projectId 项目ID
     * @return Wiki页面列表
     */
    List<WikiPage> findByProjectId(Long projectId);

    /**
     * 根据项目ID查询所有Wiki页面（分页）
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return Wiki页面分页列表
     */
    Page<WikiPage> findByProjectId(Long projectId, Pageable pageable);

    // ==================== 树状结构查询 ====================

    /**
     * 查询项目下的根页面（无父页面）
     * 根页面下没有parentId
     *
     * @param projectId 项目ID
     * @return 根页面列表（按排序序号升序）
     */
    @Query("SELECT w FROM WikiPage w WHERE w.projectId = :projectId AND w.parentId IS NULL " +
            "ORDER BY w.sortOrder ASC, w.createdAt ASC")
    List<WikiPage> findRootPages(@Param("projectId") Long projectId);

    /**
     * 根据项目ID和父页面ID查询子页面列表
     *
     * @param projectId 项目ID
     * @param parentId  父页面ID
     * @return 子页面列表（按排序序号升序）
     */
    @Query("SELECT w FROM WikiPage w WHERE w.projectId = :projectId AND w.parentId = :parentId " +
            "ORDER BY w.sortOrder ASC, w.createdAt ASC")
    List<WikiPage> findChildPages(@Param("projectId") Long projectId, @Param("parentId") Long parentId);

    /**
     * 查询指定父页面下的最大排序序号
     *
     * @param projectId 项目ID
     * @param parentId  父页面ID（null表示根页面）
     * @return 最大排序序号
     */
    @Query("SELECT COALESCE(MAX(w.sortOrder), 0) FROM WikiPage w " +
            "WHERE w.projectId = :projectId AND " +
            "(w.parentId = :parentId OR (:prentId IS NULL AND w.parentId IS NULL))")
    Integer findMaxSortOrder(@Param("projectId") Long projectId, @Param("parentId") Long parentId);

    /**
     * 统计指定页面下的子页面数量
     *
     * @param parentId 父页面ID
     * @return 子页面数量
     */
    long countByParentId(Long parentId);

    /**
     * 根据路径查询Wiki页面（支持ltree类型）
     *
     * @param projectId 项目ID
     * @param path      页面路径
     * @return Wiki页面
     */
    Optional<WikiPage> findByProjectIdAndPath(Long projectId, String path);

    /**
     * 查询路径的所有子页面（ltree匹配）
     * 使用原生SQL利用PostgreSQL的ltree特性
     *
     * @param projectId 项目ID
     * @param parentPath 父路径
     * @return 子页面列表
     */
    @Query(value = "SELECT * FROM zhiyanwiki.wiki_page " +
            "WHERE project_id = :projectId AND path <@ CAST(:parentPath AS ltree)",
            nativeQuery = true)
    List<WikiPage> findDescendantsByPath(@Param("projectId") Long projectId,
                                         @Param("parentPath") String parentPath);

    // ==================== 标题查询 ====================

    /**
     * 根据项目ID和标题查询Wiki页面（精确匹配）
     *
     * @param projectId 项目ID
     * @param title     页面标题
     * @return Wiki页面
     */
    Optional<WikiPage> findByProjectIdAndTitle(Long projectId, String title);

    /**
     * 检查项目下是否存在指定标题的页面
     *
     * @param projectId 项目ID
     * @param title     页面标题
     * @return 是否存在
     */
    boolean existsByProjectIdAndTitle(Long projectId, String title);

    /**
     * 根据项目ID和标题模糊查询Wiki页面
     *
     * @param projectId 项目ID
     * @param title     标题关键字
     * @param pageable  分页参数
     * @return Wiki页面分页列表
     */
    Page<WikiPage> findByProjectIdAndTitleContainingIgnoreCase(Long projectId, String title, Pageable pageable);

    /**
     * 根据项目ID、标题和父页面ID查询（用于同级标题检查）
     *
     * @param projectId 项目ID
     * @param title     页面标题
     * @param parentId  父页面ID
     * @return Wiki页面列表
     */
    List<WikiPage> findByProjectIdAndTitleAndParentId(Long projectId, String title, Long parentId);

    // ==================== 全文搜索 ====================

    /**
     * PostgreSQL全文搜索（使用tsvector）
     * 搜索标题和内容
     *
     * @param projectId 项目ID
     * @param keyword   搜索关键词
     * @param pageable  分页参数
     * @return 搜索结果分页
     */
    @Query(value = "SELECT w.* FROM zhiyanwiki.wiki_page w " +
            "WHERE w.project_id = :projectId AND " +
            "(to_tsvector('simple', COALESCE(w.title, '')) || " +
            " to_tsvector('simple', COALESCE(w.content, ''))) @@ plainto_tsquery('simple', :keyword) " +
            "ORDER BY ts_rank(to_tsvector('simple', COALESCE(w.title, '') || ' ' || COALESCE(w.content, '')), " +
            "                 plainto_tsquery('simple', :keyword)) DESC",
            nativeQuery = true)
    Page<WikiPage> fullTextSearch(@Param("projectId") Long projectId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    /**
     * 内容哈希查询（用于检测重复内容）
     *
     * @param projectId    项目ID
     * @param contentHash  内容哈希值
     * @return Wiki页面列表
     */
    List<WikiPage> findByProjectIdAndContentHash(Long projectId, String contentHash);

    // ==================== 页面类型查询 ====================

    /**
     * 根据项目ID和页面类型查询
     *
     * @param projectId 项目ID
     * @param pageType  页面类型
     * @param pageable  分页参数
     * @return Wiki页面分页列表
     */
    Page<WikiPage> findByProjectIdAndPageType(Long projectId, PageType pageType, Pageable pageable);

    /**
     * 查询项目下所有目录类型页面
     *
     * @param projectId 项目ID
     * @return 目录页面列表
     */
    List<WikiPage> findByProjectIdAndPageType(Long projectId, PageType pageType);

    // ==================== 用户相关查询 ====================

    /**
     * 根据创建者ID查询Wiki页面
     *
     * @param creatorId 创建者ID
     * @param pageable  分页参数
     * @return Wiki页面分页列表
     */
    Page<WikiPage> findByCreatedBy(Long creatorId, Pageable pageable);

    /**
     * 根据项目ID和创建者ID查询Wiki页面
     *
     * @param projectId 项目ID
     * @param creatorId 创建者ID
     * @return Wiki页面列表
     */
    List<WikiPage> findByProjectIdAndCreatedBy(Long projectId, Long creatorId);

    /**
     * 查询最近更新的Wiki页面
     *
     * @param projectId 项目ID
     * @param pageable  分页参数
     * @return Wiki页面列表
     */
    @Query("SELECT w FROM WikiPage w WHERE w.projectId = :projectId " +
            "ORDER BY w.updatedAt DESC")
    Page<WikiPage> findRecentlyUpdated(@Param("projectId") Long projectId, Pageable pageable);

    /**
     * 查询用户最近编辑的页面
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return Wiki页面列表
     */
    Page<WikiPage> findByUpdatedByOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    // ==================== 公开页面查询 ====================

    /**
     * 查询公开的Wiki页面（分页）
     *
     * @param pageable 分页参数
     * @return Wiki页面分页列表
     */
    @Query("SELECT w FROM WikiPage w WHERE w.isPublic = true " +
            "ORDER BY w.updatedAt DESC")
    Page<WikiPage> findPublicPages(Pageable pageable);

    /**
     * 根据项目ID查询公开的Wiki页面
     *
     * @param projectId 项目ID
     * @return Wiki页面列表
     */
    List<WikiPage> findByProjectIdAndIsPublicTrue(Long projectId);

    // ==================== 统计查询 ====================

    /**
     * 统计项目下的Wiki页面数量
     *
     * @param projectId 项目ID
     * @return 页面数量
     */
    long countByProjectId(Long projectId);

    /**
     * 统计项目下各类型页面数量
     *
     * @param projectId 项目ID
     * @param pageType  页面类型
     * @return 页面数量
     */
    long countByProjectIdAndPageType(Long projectId, PageType pageType);

    /**
     * 根据页面ID查询对应的项目ID
     *
     * @param wikiPageId 页面ID
     * @return 项目ID
     */
    @Query("SELECT w.projectId FROM WikiPage w WHERE w.id = :id")
    Optional<Long> findProjectIdById(@Param("id") Long wikiPageId);

    /**
     * 统计项目的总内容大小
     *
     * @param projectId 项目ID
     * @return 总字符数
     */
    @Query("SELECT COALESCE(SUM(w.contentSize), 0) FROM WikiPage w " +
            "WHERE w.projectId = :projectId")
    Long sumContentSizeByProjectId(@Param("projectId") Long projectId);

    // ==================== 协同编辑相关（预留） ====================

    /**
     * 查询被锁定的页面
     *
     * @param projectId 项目ID
     * @return 被锁定的页面列表
     */
    List<WikiPage> findByProjectIdAndIsLockedTrue(Long projectId);

    /**
     * 查询用户正在编辑的页面
     *
     * @param userId 用户ID
     * @return 正在编辑的页面列表
     */
    List<WikiPage> findByLockedBy(Long userId);

    /**
     * 释放超时的锁（原生SQL更新）
     *
     * @param timeoutMinutes 超时分钟数
     * @return 影响行数
     */
    @Modifying
    @Query(value = "UPDATE zhiyanwiki.wiki_page SET is_locked = false, locked_by = NULL, locked_at = NULL " +
            "WHERE is_locked = true AND locked_at < NOW() - INTERVAL ':timeout minutes'",
            nativeQuery = true)
    int releaseTimeoutLocks(@Param("timeout") int timeoutMinutes);

    // ==================== 批量删除 ====================

    /**
     * 批量删除项目下的所有Wiki页面
     *
     * @param projectId 项目ID
     */
    void deleteByProjectId(Long projectId);

    /**
     * 批量删除指定父页面下的所有子页面
     *
     * @param parentId 父页面ID
     */
    void deleteByParentId(Long parentId);

    // ==================== 版本相关 ====================

    /**
     * 查询需要归档版本的页面（recent_versions超过10个）
     * 使用PostgreSQL的JSONB函数
     *
     * @return 需要归档的页面列表
     */
    @Query(value = "SELECT * FROM zhiyanwiki.wiki_page " +
            "WHERE jsonb_array_length(recent_versions) > 10",
            nativeQuery = true)
    List<WikiPage> findPagesNeedingArchival();
}
