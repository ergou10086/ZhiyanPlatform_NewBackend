package hbnu.project.zhiyanbackend.wiki.service.impl;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.wiki.model.dto.*;
import hbnu.project.zhiyanbackend.wiki.model.entity.ChangeStats;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiVersionHistory;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.repository.WikiVersionHistoryRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiPageService;
import hbnu.project.zhiyanbackend.wiki.utils.WikiDiffUtils;
import hbnu.project.zhiyanbackend.message.service.MessageSendService;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wiki页面服务实现类
 * 负责Wiki文档页面的管理
 *
 * @author ErgouTree
 * @rewrite ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiPageServiceImpl implements WikiPageService {

    @Resource
    private WikiPageRepository wikiPageRepository;

    @Resource
    private WikiVersionHistoryRepository versionHistoryRepository;

    @Resource
    private WikiDiffUtils wikiDiffUtils;

    @Resource
    private ApplicationContext applicationContext;

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private MessageSendService messageSendService;

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

        // 发送Wiki页面创建通知
        try {
            messageSendService.notifyWikiPageCreated(page, dto.getCreatorId());
        } catch (Exception e) {
            log.error("发送Wiki页面创建通知失败: pageId={}", page.getId(), e);
            // 通知发送失败不影响主流程
        }

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

        // 发送Wiki页面更新通知（仅在内容有变化时发送）
        if (saved.getPageType() == PageType.DOCUMENT && content != null) {
            try {
                messageSendService.notifyWikiPageUpdated(saved, editorId, changeDesc);
            } catch (Exception e) {
                log.error("发送Wiki页面更新通知失败: pageId={}", saved.getId(), e);
                // 通知发送失败不影响主流程
            }
        }

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
        try {
            WikiVersionHistory history = WikiVersionHistory.builder()
                    .wikiPageId(wikiPageId)
                    .projectId(projectId)
                    .version(recentVersion.getVersion())
                    .contentDiff(recentVersion.getContentDiff())
                    .changeDescription(recentVersion.getChangeDescription())
                    .build();

            history.setCreatedBy(recentVersion.getEditorId());
            history.setCreatedAt(recentVersion.getCreatedAt());
            history.setAddedLines(recentVersion.getAddedLines());
            history.setDeletedLines(recentVersion.getDeletedLines());
            history.setChangedChars(recentVersion.getChangedChars());
            history.setContentHash(recentVersion.getContentHash());

            versionHistoryRepository.save(history);
            log.info("归档旧版本成功: wikiPageId={}, version={}", wikiPageId, recentVersion.getVersion());
        } catch (Exception e) {
            log.error("归档旧版本失败: wikiPageId={}, version={}", wikiPageId, recentVersion.getVersion(), e);
        }
    }

    /**
     * 更新页面排序
     *
     * @param pageId    页面ID
     * @param sortOrder 新的排序序号
     */
    @Override
    @Transactional
    public void updateSortOrder(Long pageId, Integer sortOrder) {
        WikiPage page = wikiPageRepository.findById(pageId).orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        page.setSortOrder(sortOrder);
        wikiPageRepository.save(page);

        log.info("更新Wiki页面排序: pageId={}, sortOrder={}", pageId, sortOrder);
    }

    /**
     * 删除Wiki页面
     *
     * @param pageId     页面ID
     * @param operatorId 操作者id
     */
    @Override
    public void deleteWikiPage(Long pageId, Long operatorId) {
        WikiPage wikiPage = wikiPageRepository.findById(pageId).orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        // 检查是否有子页面
        long childrenCount = wikiPageRepository.countByParentId(pageId);
        if (childrenCount > 0) {
            throw new ServiceException("该页面下还有子页面，请先删除子页面");
        }

        // 删除其版本历史
        versionHistoryRepository.deleteByWikiPageId(pageId);

        // 发送Wiki页面删除通知（在删除前发送，因为删除后无法获取页面信息）
        try {
            messageSendService.notifyWikiPageDeleted(wikiPage, operatorId);
        } catch (Exception e) {
            log.error("发送Wiki页面删除通知失败: pageId={}", wikiPage.getId(), e);
            // 通知发送失败不影响主流程
        }

        // 删除元数据
        wikiPageRepository.delete(wikiPage);
        log.info("删除Wiki页面成功: id={}, title={}", wikiPage.getId(), wikiPage.getTitle());
    }

    /**
     * 递归删除目录及其所有子页面
     *
     * @param pageId 页面ID
     * @param operatorId 操作者ID
     */
    @Override
    public void deletePageRecursively(Long pageId, Long operatorId) {
        WikiPage wikiPage = wikiPageRepository.findById(pageId).orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        // 如果是目录，递归删除所有子页面
        if (wikiPage.getPageType() == PageType.DIRECTORY) {
            List<WikiPage> childrenPage = wikiPageRepository.findChildPages(wikiPage.getProjectId(), pageId);
            for (WikiPage child : childrenPage) {
                // 通过代理对象调用自身，也就是通过容器获取代理对象，再调用方法
                applicationContext.getBean(WikiPageService.class).deletePageRecursively(child.getId(), operatorId);
            }
        }

        // 删除对应的页面的版面历史
        versionHistoryRepository.deleteByWikiPageId(pageId);

        // 发送Wiki页面删除通知（在删除前发送，因为删除后无法获取页面信息）
        // 只在最顶层页面发送一次通知，避免递归删除时发送过多通知
        if (operatorId != null) {
            try {
                messageSendService.notifyWikiPageDeleted(wikiPage, operatorId);
            } catch (Exception e) {
                log.error("发送Wiki页面删除通知失败: pageId={}", wikiPage.getId(), e);
                // 通知发送失败不影响主流程
            }
        }

        // 删除当前页面
        wikiPageRepository.delete(wikiPage);
        log.info("递归删除Wiki页面: id={}, title={}", wikiPage.getId(), wikiPage.getTitle());
    }

    /**
     * 获取项目的Wiki树状结构
     *
     * @param projectId 项目ID
     * @return 树状结构列表
     */
    @Override
    public List<WikiPageTreeDTO> getProjectWikiTree(Long projectId) {
        // 获取所有根页面
        List<WikiPage> rootPages = wikiPageRepository.findRootPages(projectId);

        // 构建树状结构
        return rootPages.stream()
                .map(this::buildWikiTree)
                .toList();
    }

    /**
     * 递归构建Wiki树
     *
     * @param page Wiki页面
     * @return 树节点DTO
     */
    @Override
    public WikiPageTreeDTO buildWikiTree(WikiPage page) {
        // 构建Wiki树
        WikiPageTreeDTO dto = WikiPageTreeDTO.builder()
                .id(String.valueOf(page.getId()))
                .title(page.getTitle())
                .parentId(page.getParentId() != null ? String.valueOf(page.getParentId()) : null)
                .path(page.getPath())
                .sortOrder(page.getSortOrder())
                .isPublic(page.getIsPublic())
                .pageType(page.getPageType().name())
                .currentVersion(page.getCurrentVersion())
                .contentSummary(page.getContentSummary())
                .createdAt(page.getCreatedAt() != null ? page.getCreatedAt().toString() : null)
                .updatedAt(page.getUpdatedAt() != null ? page.getUpdatedAt().toString() : null)
                .build();

        // 获取子页面
        List<WikiPage> children = wikiPageRepository.findChildPages(page.getProjectId(), page.getId());
        dto.setHasChildren(!children.isEmpty());
        dto.setChildrenCount(children.size());

        // 递归构建子树
        if (!children.isEmpty()) {
            List<WikiPageTreeDTO> childrenDtos = children.stream()
                    .map(this::buildWikiTree)
                    .toList();

            dto.setChildren(childrenDtos);
        }else{
            dto.setChildren(new ArrayList<>());
        }

        return dto;
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
        if (parentId == null) {
            return "/" + title;
        }

        WikiPage parent = wikiPageRepository.findById(parentId).orElseThrow(() -> new ServiceException("父页面不存在"));

        return parent.getPath() + "/" + title;
    }

    /**
     * 获取Wiki页面详情（包含内容）- 返回DTO
     *
     * @param pageId 页面ID
     * @return Wiki页面详情DTO
     */
    @Override
    public WikiPageDetailDTO getWikiPageWithContent(Long pageId) {
        WikiPage page = wikiPageRepository.findById(pageId).orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        WikiPageDetailDTO.WikiPageDetailDTOBuilder builder = WikiPageDetailDTO.builder()
                .id(String.valueOf(page.getId()))
                .projectId(String.valueOf(page.getProjectId()))
                .title(page.getTitle())
                .pageType(page.getPageType().name())
                .parentId(page.getParentId() != null ? String.valueOf(page.getParentId()) : null)
                .path(page.getPath())
                .contentSummary(page.getContentSummary())
                .currentVersion(page.getCurrentVersion())
                .contentSize(page.getContentSize())
                .isPublic(page.getIsPublic())
                .isLocked(page.getIsLocked())
                .lockedBy(page.getLockedBy() != null ? String.valueOf(page.getLockedBy()) : null)
                .creatorId(String.valueOf(page.getCreatedBy()))
                .lastEditorId(page.getUpdatedBy() != null ? String.valueOf(page.getUpdatedBy()) : null)
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt());

        // 如果是文档类型，获取内容
        if (page.getPageType() == PageType.DOCUMENT) {
            builder.content(page.getContent());
        }

        return builder.build();
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
        Page<WikiPage> pages = wikiPageRepository.findByProjectIdAndTitleContainingIgnoreCase(projectId, keyword, pageable);

        return pages.map(page -> WikiSearchDTO.builder()
                .id(page.getId())
                .title(page.getTitle())
                .path(page.getPath())
                .pageType(page.getPageType().name())
                .contentSummary(page.getContentSummary())
                .updatedAt(page.getUpdatedAt())
                .lastEditorId(page.getUpdatedBy() != null ? page.getUpdatedBy() : null)
                .build());
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
        log.info("[Wiki内容搜索] 开始搜索: projectId={}, keyword={}", projectId, keyword);

        // 使用PostgreSQL全文搜索
        Page<WikiPage> pages = wikiPageRepository.fullTextSearch(projectId, keyword, pageable);
        log.info("[Wiki内容搜索] PostgreSQL返回结果数: {}", pages.getTotalElements());

        return pages.map(page -> {
            // 提取匹配内容的上下文作为摘要
            String summary = extractMatchContext(page.getContent(), keyword, 200);

            return WikiSearchDTO.builder()
                    .id(page.getId())
                    .title(page.getTitle())
                    .path(page.getPath())
                    .pageType(page.getPageType().name())
                    .contentSummary(summary)
                    .updatedAt(page.getUpdatedAt())
                    .lastEditorId(page.getUpdatedBy() != null ? page.getUpdatedBy() : null)
                    .build();
        });
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
        if (content == null || content.isEmpty() || keyword == null || keyword.isEmpty()) {
            return "";
        }

        // 处理多关键字情况（空格分隔）
        String[] keywords = keyword.split("\\s+");
        Pattern pattern = Pattern.compile(String.join("|", keywords), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        // 找到第一个匹配项
        if (matcher.find()) {
            int start = Math.max(0, matcher.start() - maxLength / 2);
            int end = Math.min(content.length(), matcher.end() + maxLength / 2);

            // 截取上下文并添加省略号
            String context = content.substring(start, end);
            if (start > 0) {
                context = "..." + context;
            }
            if (end < content.length()) {
                context = context + "...";
            }

            // 高亮关键字（可根据前端需求调整格式）
            for (String kw : keywords) {
                context = context.replaceAll("(?i)" + Pattern.quote(kw), "<em>" + kw + "</em>");
            }

            return context;
        }

        // 如果内容中没有关键字但标题中有，返回内容开头
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }

    /**
     * 获取项目的Wiki统计信息
     *
     * @param projectId 项目ID
     * @return 统计信息
     */
    @Override
    public WikiStatisticsDTO getProjectStatistics(Long projectId) {
        log.info("获取项目[{}]的Wiki统计信息", projectId);

        if (projectId == null) {
            log.warn("projectId为空，返回空统计信息");
            return WikiStatisticsDTO.builder()
                    .projectId("0")
                    .totalPages(0L)
                    .documentCount(0L)
                    .directoryCount(0L)
                    .totalContentSize(0L)
                    .contributorCount(0)
                    .recentUpdates(0L)
                    .totalVersions(0L)
                    .contributorStats(new HashMap<>())
                    .build();
        }

        // 获取所有页面
        List<WikiPage> allPages = wikiPageRepository.findByProjectId(projectId);
        log.debug("项目[{}]共有{}个Wiki页面", projectId, allPages.size());

        // 统计文档和目录数量
        long documentCount = allPages.stream()
                .filter(p -> p.getPageType() == PageType.DOCUMENT)
                .count();
        long directoryCount = allPages.stream()
                .filter(p -> p.getPageType() == PageType.DIRECTORY)
                .count();

        log.debug("项目[{}]文档数: {}, 目录数: {}", projectId, documentCount, directoryCount);

        // 统计总内容大小
        long totalContentSize = allPages.stream()
                .filter(p -> p.getContentSize() != null)
                .mapToLong(WikiPage::getContentSize)
                .sum();

        // 统计贡献者
        Set<Long> contributors = new HashSet<>();
        allPages.stream()
                .map(WikiPage::getCreatedBy)
                .filter(Objects::nonNull)
                .forEach(contributors::add);
        allPages.stream()
                .map(WikiPage::getUpdatedBy)
                .filter(Objects::nonNull)
                .forEach(contributors::add);

        // 统计最近30天更新
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentUpdates = allPages.stream()
                .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(thirtyDaysAgo))
                .count();

        // 统计各贡献者编辑次数
        Map<String, Integer> contributorStats = new HashMap<>();
        for (WikiPage page : allPages) {
            if (page.getCreatedBy() != null) {
                String creatorId = String.valueOf(page.getCreatedBy());
                contributorStats.put(creatorId, contributorStats.getOrDefault(creatorId, 0) + 1);
            }
        }

        // 统计总版本数
        long totalVersions = allPages.stream()
                .filter(p -> p.getCurrentVersion() != null)
                .mapToLong(p -> p.getCurrentVersion().longValue())
                .sum();

        // 加上历史版本数
        totalVersions += versionHistoryRepository.countByProjectId(projectId);

        WikiStatisticsDTO result = WikiStatisticsDTO.builder()
                .projectId(String.valueOf(projectId))
                .totalPages((long) allPages.size())
                .documentCount(documentCount)
                .directoryCount(directoryCount)
                .totalContentSize(totalContentSize)
                .contributorCount(contributors.size())
                .recentUpdates(recentUpdates)
                .totalVersions(totalVersions)
                .contributorStats(contributorStats)
                .build();

        log.info("项目[{}]的Wiki统计信息: 总页面数={}, 文档数={}, 目录数={}, 贡献者数={}",
                projectId, result.getTotalPages(), result.getDocumentCount(),
                result.getDirectoryCount(), result.getContributorCount());

        return result;
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
        WikiPage sourcePage = wikiPageRepository.findById(sourcePageId)
                .orElseThrow(() -> new ServiceException("源页面不存在"));

        // 生成新标题
        if (!StringUtils.hasText(newTitle)) {
            newTitle = "副本-" + sourcePage.getTitle();
        }

        // 创建DTO
        CreateWikiPageDTO dto = CreateWikiPageDTO.builder()
                .projectId(sourcePage.getProjectId())
                .title(newTitle)
                .pageType(sourcePage.getPageType())
                .parentId(targetParentId)
                .isPublic(sourcePage.getIsPublic())
                .creatorId(userId)
                .changeDescription("复制自: " + sourcePage.getTitle())
                .build();

        // 如果是文档类型，复制内容
        if (sourcePage.getPageType() == PageType.DOCUMENT) {
            dto.setContent(sourcePage.getContent());
        }

        // 通过容器获取代理对象，再调用方法
        WikiPage newPage = applicationContext.getBean(WikiPageService.class).createWikiPage(dto);

        log.info("复制Wiki页面: sourceId={}, newId={}", sourcePageId, newPage.getId());
        return newPage;
    }

    /**
     * 移动Wiki页面（修改父页面）
     *
     * @param pageId      页面ID
     * @param newParentId 新父页面ID
     * @param operatorId 操作者id
     */
    @Override
    public void moveWikiPage(Long pageId, Long newParentId, Long operatorId) {
        WikiPage page = wikiPageRepository.findById(pageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        // 验证新父页面
        if (newParentId != null) {
            WikiPage newParent = wikiPageRepository.findById(newParentId)
                    .orElseThrow(() -> new ServiceException("新父页面不存在"));

            if (newParent.getPageType() == PageType.DOCUMENT) {
                throw new ServiceException("不能移动到文档节点下");
            }

            if (!newParent.getProjectId().equals(page.getProjectId())) {
                throw new ServiceException("不能移动到其他项目");
            }
        }

        // 更新父页面和路径
        page.setParentId(newParentId);
        page.setPath(calculatePath(page.getProjectId(), newParentId, page.getTitle()));
        page.setUpdatedBy(operatorId);
        wikiPageRepository.save(page);

        log.info("移动Wiki页面成功: id={}, newParentId={}", pageId, newParentId);
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
        Pageable pageable = PageRequest.of(0, limit);
        Page<WikiPage> pages = wikiPageRepository.findRecentlyUpdated(projectId, pageable);

        return pages.getContent().stream()
                .map(page -> WikiPageTreeDTO.builder()
                        .id(String.valueOf(page.getId()))
                        .title(page.getTitle())
                        .path(page.getPath())
                        .pageType(page.getPageType().name())
                        .contentSummary(page.getContentSummary())
                        .currentVersion(page.getCurrentVersion())
                        .updatedAt(page.getUpdatedAt() != null ? page.getUpdatedAt().toString() : null)
                        .build())
                .toList();
    }

    /**
     * 全文搜索Wiki内容
     * 整合PostgreSQL全文搜索功能
     * 同时搜索标题和内容，返回带上下文的结果
     *
     * @param projectId 项目id
     * @param keyword   关键字
     * @param pageable  分页
     * @return WikiSearchDTO
     */
    @Override
    public Page<WikiSearchDTO> fullTextSearch(Long projectId, String keyword, Pageable pageable) {
        // 使用已有的PostgreSQL全文搜索查询
        Page<WikiPage> pages = wikiPageRepository.fullTextSearch(projectId, keyword, pageable);

        return pages.map(page -> {
            WikiSearchDTO dto = new WikiSearchDTO();
            dto.setId(page.getId());
            dto.setProjectId(page.getProjectId());
            dto.setTitle(page.getTitle());
            dto.setPageType(page.getPageType().name());
            dto.setPath(page.getPath());
            dto.setUpdatedAt(page.getUpdatedAt());
            dto.setLastEditorId(page.getUpdatedBy());

            // 提取包含关键字的上下文
            dto.setContentContext(extractMatchContext(page.getContent(), keyword, 200));

            // 计算简单得分（实际可根据PostgreSQL的ts_rank调整）
            dto.setScore(calculateSearchScore(page.getTitle(), page.getContent(), keyword));

            return dto;
        });
    }

    /**
     * 计算搜索得分（简单实现）
     * 标题匹配权重高于内容匹配
     */
    private Float calculateSearchScore(String title, String content, String keyword) {
        float score = 0.0f;
        String[] keywords = keyword.split("\\s+");

        // 标题匹配权重更高
        for (String kw : keywords) {
            if (title.toLowerCase().contains(kw.toLowerCase())) {
                score += 2.0f; // 标题匹配权重
            }
            if (content != null && content.toLowerCase().contains(kw.toLowerCase())) {
                score += 1.0f; // 内容匹配权重
            }
        }

        // 归一化得分
        return Math.min(10.0f, score);
    }
}
