package hbnu.project.zhiyanbackend.wiki.service;

import hbnu.project.zhiyanbackend.wiki.model.dto.WikiVersionDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;

import java.util.List;

/**
 * Wiki文档内容服务
 * 负责Wiki内容的增删改查和版本管理
 *
 * @author ErgouTree
 * @rewrite ErgouTree
 */
public interface WikiContentService {

    /**
     * 获取指定版本的完整内容
     * 通过逆向应用差异补丁来重建历史版本
     *
     * @param wikiPageId    Wiki页面ID
     * @param targetVersion 目标版本号
     * @return 指定版本的完整内容
     */
    String getVersionContent(Long wikiPageId, Integer targetVersion);

    /**
     * 获取最近的版本历史列表（最多10个版本）
     * 这些版本存储在主内容表中，未被归档
     *
     * @param wikiPageId Wiki页面ID
     * @return 最近版本历史列表
     */
    List<WikiPage.RecentVersionInfo> getRecentVersions(Long wikiPageId);

    /**
     * 获取所有版本历史（包括最近版本和已归档版本）
     *
     * @param wikiPageId Wiki页面ID
     * @return 所有版本历史的DTO列表
     */
    List<WikiVersionDTO> getAllVersionHistory(Long wikiPageId);

    /**
     * 比较两个版本之间的内容差异
     *
     * @param wikiPageId Wiki页面ID
     * @param version1   第一个版本号
     * @param version2   第二个版本号
     * @return 两个版本的差异文本
     */
    String compareVersions(Long wikiPageId, Integer version1, Integer version2);

    /**
     * 获取版本历史列表（包含详细信息）
     *
     * @param wikiPageId Wiki页面ID
     * @return 版本历史DTO列表
     */
    List<WikiVersionDTO> getVersionHistory(Long wikiPageId);

    /**
     * 删除指定Wiki页面的所有版本历史
     *
     * @param wikiPageId Wiki页面ID
     */
    void deleteVersionHistory(Long wikiPageId);

    /**
     * 批量删除指定项目下的所有版本历史
     * 用于项目删除时的级联操作
     *
     * @param projectId 项目ID
     */
    void deleteByProjectId(Long projectId);
}
