package hbnu.project.zhiyanbackend.wiki.service;

import hbnu.project.zhiyanbackend.wiki.model.dto.*;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Wiki页面服务类
 * 负责Wiki文档页面的管理
 *
 * @author ErgouTree
 * @rewrite ErgouTree
 */
public interface WikiPageService {

    /**
     * 创建 Wiki 页面
     * 支持创建目录节点和文档节点
     *
     * @param dto CreateWikiPageDTO Wiki页面创建dto
     * @return WikiPage Wiki页面
     */
    WikiPage createWikiPage(CreateWikiPageDTO dto);


    /**
     * 更新Wiki页面
     *
     * @param pageId        页面ID
     * @param title         新标题
     * @param content       新内容
     * @param changeDesc    修改说明
     * @param editorId      编辑者ID
     * @return 更新后的页面
     */
    WikiPage updateWikiPage(Long pageId, String title, String content, String changeDesc, Long editorId);

    /**
     * 将超过最近版本上限的旧版本归档到历史表
     *
     * @param wikiPageId 要归档的wiki页面id
     * @param projectId 对应项目id
     * @param recentVersion 最近版本
     */
    void archiveOldVersion(Long wikiPageId, Long projectId, WikiPage.RecentVersionInfo recentVersion);

    /**
     * 更新页面排序
     *
     * @param pageId    页面ID
     * @param sortOrder 新的排序序号
     */
    void updateSortOrder(Long pageId, Integer sortOrder);

    /**
     * 删除Wiki页面
     *
     * @param pageId 页面ID
     */
    void deleteWikiPage(Long pageId, Long operatorId);

    /**
     * 递归删除目录及其所有子页面
     *
     * @param pageId 页面ID
     */
    void deletePageRecursively(Long pageId);

    /**
     * 获取项目的Wiki树状结构
     *
     * @param projectId 项目ID
     * @return 树状结构列表
     */
    List<WikiPageTreeDTO> getProjectWikiTree(Long projectId);

    /**
     * 递归构建Wiki树
     *
     * @param page Wiki页面
     * @return 树节点DTO
     */
    WikiPageTreeDTO buildWikiTree(WikiPage page);

    /**
     * 计算页面路径
     *
     * @param projectId 项目ID
     * @param parentId  父页面ID
     * @param title     页面标题
     * @return 页面路径
     */
    String calculatePath(Long projectId, Long parentId, String title);

    /**
     * 获取Wiki页面详情（包含内容）- 返回DTO
     *
     * @param pageId 页面ID
     * @return Wiki页面详情DTO
     */
    WikiPageDetailDTO getWikiPageWithContent(Long pageId);

    /**
     * 搜索Wiki页面（根据标题）
     *
     * @param projectId 项目ID
     * @param keyword   搜索关键字
     * @param pageable  分页参数
     * @return 搜索结果分页
     */
    Page<WikiSearchDTO> searchByTitle(Long projectId, String keyword, Pageable pageable);

    /**
     * 搜索Wiki页面（根据内容 - PostgreSQL全文搜索）
     *
     * @param projectId 项目ID
     * @param keyword   搜索关键字
     * @param pageable  分页参数
     * @return 搜索结果列表
     */
    Page<WikiSearchDTO> searchByContent(Long projectId, String keyword, Pageable pageable);

    /**
     * 提取匹配关键字的上下文
     *
     * @param content     完整内容
     * @param keyword     关键字
     * @param maxLength   最大长度
     * @return 上下文摘要
     */
    String extractMatchContext(String content, String keyword, int maxLength);

    /**
     * 获取项目的Wiki统计信息
     *
     * @param projectId 项目ID
     * @return 统计信息
     */
    WikiStatisticsDTO getProjectStatistics(Long projectId);

    /**
     * 复制Wiki页面
     *
     * @param sourcePageId 源页面ID
     * @param targetParentId 目标父页面ID（null表示根目录）
     * @param newTitle 新标题（null表示使用"副本-原标题"）
     * @param userId 操作用户ID
     * @return 新创建的页面
     */
    WikiPage copyPage(Long sourcePageId, Long targetParentId, String newTitle, Long userId);

    /**
     * 移动Wiki页面（修改父页面）
     *
     * @param pageId      页面ID
     * @param newParentId 新父页面ID
     */
    void moveWikiPage(Long pageId, Long newParentId, Long operatorId);

    /**
     * 获取最近更新的Wiki页面
     *
     * @param projectId 项目ID
     * @param limit     数量限制
     * @return 最近更新的页面列表
     */
    List<WikiPageTreeDTO> getRecentlyUpdated(Long projectId, int limit);
}
