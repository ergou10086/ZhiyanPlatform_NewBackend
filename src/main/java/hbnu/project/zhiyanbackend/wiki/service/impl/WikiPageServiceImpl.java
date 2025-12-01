package hbnu.project.zhiyanbackend.wiki.service.impl;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.wiki.model.dto.*;
import hbnu.project.zhiyanbackend.wiki.model.entity.ChangeStats;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.repository.WikiVersionHistoryRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiPageService;
import hbnu.project.zhiyanbackend.wiki.utils.WikiDiffUtils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wiki页面服务实现类
 * 负责Wiki文档页面的管理
 *
 * @author ErgouTree
 * @rewrite ErgouTree
 */
@Slf4j
@Service
public class WikiPageServiceImpl implements WikiPageService {

    @Resource
    private WikiPageRepository wikiPageRepository;

    @Resource
    private WikiVersionHistoryRepository versionHistoryRepository;

    @Resource
    private WikiDiffUtils wikiDiffUtils;

    /**
     * 创建 Wiki 页面
     * 支持创建目录节点和文档节点
     *
     * @param dto CreateWikiPageDTO Wiki页面创建dto
     * @return WikiPage Wiki页面
     */
    @Override
    @Transactional
    public WikiPage createWikiPage(CreateWikiPageDTO dto) {
        // 1. 验证父页面（如果有）
        if (dto.getParentId() != null) {
            WikiPage parent = wikiPageRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ServiceException("父页面不存在"));

            // 验证父页面是否是目录类型
            if (parent.getPageType() == PageType.DOCUMENT) {
                throw new ServiceException("不能在文档节点下创建子节点，请选择目录节点");
            }

            // 验证是否属于同一项目
            if (!parent.getProjectId().equals(dto.getProjectId())) {
                throw new ServiceException("父页面不属于该项目");
            }
        }

        // 2. 计算路径
        String path = calculatePath(dto.getProjectId(), dto.getParentId(), dto.getTitle());

        // 3. 计算排序序号
        Integer sortOrder = dto.getSortOrder();
        if (sortOrder == null) {
            Integer maxOrder = wikiPageRepository.findMaxSortOrder(dto.getProjectId(), dto.getParentId());
            sortOrder = (maxOrder != null ? maxOrder : 0) + 1;
        }

        // 4. 创建Wiki页面实体
        WikiPage page = WikiPage.builder()
                .projectId(dto.getProjectId())
                .title(dto.getTitle())
                .pageType(dto.getPageType())
                .parentId(dto.getParentId())
                .path(path)
                .sortOrder(sortOrder)
                .isPublic(dto.getIsPublic())
                .build();

        // 设置创建者（通过BaseAuditEntity的createdBy字段）
        page.setCreatedBy(dto.getCreatorId());
        page.setUpdatedBy(dto.getCreatorId());

        // 5. 如果是文档类型，设置内容
        if (dto.getPageType() == PageType.DOCUMENT) {
            String content = dto.getContent() != null ? dto.getContent() : "";
            page.setContent(content);
            page.setContentHash(wikiDiffUtils.calculateHash(content));
            page.setContentSize(content.length());
            page.setCurrentVersion(1);

            // 生成摘要（前200字符）
            if (!content.isEmpty()) {
                String summary = content.length() > 200 ?
                        content.substring(0, 200) : content;
                page.setContentSummary(summary);
            }

            // 初始化最近版本列表
            page.setRecentVersions(new ArrayList<>());
        }

        page = wikiPageRepository.save(page);
        log.info("创建Wiki页面成功: id={}, title={}, type={}", page.getId(), page.getTitle(), page.getPageType());

        return page;
    }

    /**
     * 更新Wiki页面
     *
     * @param pageId     页面ID
     * @param title      新标题
     * @param content    新内容
     * @param changeDesc 修改说明
     * @param editorId   编辑者ID
     * @return 更新后的页面
     */
    @Override
    public WikiPage updateWikiPage(Long pageId, String title, String content, String changeDesc, Long editorId) {
        WikiPage wikiPage = wikiPageRepository.findById(pageId).orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        // 如果需要更新标题
        if (StringUtils.hasText(title) && !title.equals(wikiPage.getTitle())) {
            wikiPage.setTitle(title);
            // 重新计算路径
            wikiPage.setPath(calculatePath(wikiPage.getProjectId(), wikiPage.getParentId(), title));
        }

        // 如果是文档类型，可以对其更新内容
        if (wikiPage.getPageType() == PageType.DOCUMENT && content != null) {
            String oldContent = wikiPage.getContent() != null ? wikiPage.getContent() : "";
            String newContent = content;

            // 新内容的哈希值
            String newHash = wikiDiffUtils.calculateHash(newContent);

            // 如果内容有变化，创建新版本
            if (!newHash.equals(wikiPage.getContentHash())) {
                // 计算差异和统计
                String diff = wikiDiffUtils.calculateDiff(oldContent, newContent);
                ChangeStats stats = wikiDiffUtils.calculateStats(oldContent, newContent);

                // 创建新版本记录
                int newVersion = wikiPage.getCurrentVersion() + 1;
                WikiPage.RecentVersionInfo recentVersion = WikiPage.RecentVersionInfo.builder()
                        .version(newVersion)
                        .contentDiff(diff)
                        .changeDescription(StringUtils.hasText(changeDesc) ? changeDesc : "Update content")
                        .editorId(editorId)
                        .createdAt(LocalDateTime.now())
                        .addedLines(stats.getAddedLines())
                        .deletedLines(stats.getDeletedLines())
                        .changedChars(stats.getChangedChars())
                        .contentHash(newHash)
                        .build();

                // 维护最近版本列表
                List<WikiPage.RecentVersionInfo> versions = wikiPage.getRecentVersions();
                if (versions == null) {
                    versions = new ArrayList<>();
                }
                versions.add(recentVersion);

                // 超过最大保留数量时，将最旧的版本归档到历史表
                if (versions.size() > 10) {
                    WikiPage.RecentVersionInfo oldest = versions.removeFirst();
                    archiveOldVersion(wikiPage.getId(), wikiPage.getProjectId(), oldest);
                }

                wikiPage.setRecentVersions(versions);
                wikiPage.setCurrentVersion(newVersion);
            }

            // 更新内容
            wikiPage.setContent(newContent);
            wikiPage.setContentHash(newHash);
            wikiPage.setContentSize(newContent.length());

            // 更新摘要
            String summary = newContent.length() > 200 ? newContent.substring(0, 200) : newContent;
            wikiPage.setContentSummary(summary);
        }

        wikiPage.setUpdatedBy(editorId);
        WikiPage saved = wikiPageRepository.save(wikiPage);

        log.info("更新Wiki页面成功: id={}, title={}", wikiPage.getId(), wikiPage.getTitle());

        return saved;
    }

    /**
     * 将超过最近版本上限的旧版本归档到历史表
     *
     * @param wikiPageId    要归档的wiki页面id
     * @param projectId     对应项目id
     * @param recentVersion 最近版本
     */
    @Override
    public void archiveOldVersion(Long wikiPageId, Long projectId, WikiPage.RecentVersionInfo recentVersion) {

    }

    /**
     * 更新页面排序
     *
     * @param pageId    页面ID
     * @param sortOrder 新的排序序号
     */
    @Override
    public void updateSortOrder(Long pageId, Integer sortOrder) {

    }

    /**
     * 删除Wiki页面
     *
     * @param pageId     页面ID
     * @param operatorId
     */
    @Override
    public void deleteWikiPage(Long pageId, Long operatorId) {

    }

    /**
     * 递归删除目录及其所有子页面
     *
     * @param pageId 页面ID
     */
    @Override
    public void deletePageRecursively(Long pageId) {

    }

    /**
     * 获取项目的Wiki树状结构
     *
     * @param projectId 项目ID
     * @return 树状结构列表
     */
    @Override
    public List<WikiPageTreeDTO> getProjectWikiTree(Long projectId) {
        return List.of();
    }

    /**
     * 递归构建Wiki树
     *
     * @param page Wiki页面
     * @return 树节点DTO
     */
    @Override
    public WikiPageTreeDTO buildWikiTree(WikiPage page) {
        return null;
    }

    /**
     * 计算页面路径
     *
     * @param projectId 项目ID
     * @param parentId  父页面ID
     * @param title     页面标题
     * @return 页面路径
     */
    @Override
    public String calculatePath(Long projectId, Long parentId, String title) {
        return "";
    }

    /**
     * 获取Wiki页面详情（包含内容）- 返回DTO
     *
     * @param pageId 页面ID
     * @return Wiki页面详情DTO
     */
    @Override
    public WikiPageDetailDTO getWikiPageWithContent(Long pageId) {
        return null;
    }

    /**
     * 搜索Wiki页面（根据标题）
     *
     * @param projectId 项目ID
     * @param keyword   搜索关键字
     * @param pageable  分页参数
     * @return 搜索结果分页
     */
    @Override
    public Page<WikiSearchDTO> searchByTitle(Long projectId, String keyword, Pageable pageable) {
        return null;
    }

    /**
     * 搜索Wiki页面（根据内容 - PostgreSQL全文搜索）
     *
     * @param projectId 项目ID
     * @param keyword   搜索关键字
     * @param pageable  分页参数
     * @return 搜索结果列表
     */
    @Override
    public Page<WikiSearchDTO> searchByContent(Long projectId, String keyword, Pageable pageable) {
        return null;
    }

    /**
     * 提取匹配关键字的上下文
     *
     * @param content   完整内容
     * @param keyword   关键字
     * @param maxLength 最大长度
     * @return 上下文摘要
     */
    @Override
    public String extractMatchContext(String content, String keyword, int maxLength) {
        return "";
    }

    /**
     * 获取项目的Wiki统计信息
     *
     * @param projectId 项目ID
     * @return 统计信息
     */
    @Override
    public WikiStatisticsDTO getProjectStatistics(Long projectId) {
        return null;
    }

    /**
     * 复制Wiki页面
     *
     * @param sourcePageId   源页面ID
     * @param targetParentId 目标父页面ID（null表示根目录）
     * @param newTitle       新标题（null表示使用"副本-原标题"）
     * @param userId         操作用户ID
     * @return 新创建的页面
     */
    @Override
    public WikiPage copyPage(Long sourcePageId, Long targetParentId, String newTitle, Long userId) {
        return null;
    }

    /**
     * 移动Wiki页面（修改父页面）
     *
     * @param pageId      页面ID
     * @param newParentId 新父页面ID
     * @param operatorId
     */
    @Override
    public void moveWikiPage(Long pageId, Long newParentId, Long operatorId) {

    }

    /**
     * 获取最近更新的Wiki页面
     *
     * @param projectId 项目ID
     * @param limit     数量限制
     * @return 最近更新的页面列表
     */
    @Override
    public List<WikiPageTreeDTO> getRecentlyUpdated(Long projectId, int limit) {
        return List.of();
    }
}
