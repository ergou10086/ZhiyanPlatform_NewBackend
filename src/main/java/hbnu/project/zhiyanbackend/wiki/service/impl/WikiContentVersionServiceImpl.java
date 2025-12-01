package hbnu.project.zhiyanbackend.wiki.service.impl;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiVersionDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiVersionHistory;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.repository.WikiVersionHistoryRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiContentVersionService;
import hbnu.project.zhiyanbackend.wiki.utils.WikiDiffUtils;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Wiki内容版本管理服务实现类
 * 负责Wiki内容的版本管理、历史版本查询、版本对比等功能
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiContentVersionServiceImpl implements WikiContentVersionService {

    @Resource
    private WikiPageRepository wikiPageRepository;

    @Resource
    private WikiVersionHistoryRepository versionHistoryRepository;

    @Resource
    private WikiDiffUtils wikiDiffUtils;

    // 保留最近 10 个版本
    private static final int MAX_RECENT_VERSIONS = 10;

    /**
     * 获取指定版本的完整内容
     * 通过逆向应用差异补丁来重建历史版本
     *
     * @param wikiPageId    Wiki页面ID
     * @param targetVersion 目标版本号
     * @return 指定版本的完整内容
     */
    @Override
    public String getVersionContent(Long wikiPageId, Integer targetVersion) {
        // 获取当前最新版本内容
        WikiPage current = wikiPageRepository.findById(wikiPageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        // 如果是当前版本，直接返回
        if (targetVersion.equals(current.getCurrentVersion())) {
            return current.getContent() != null ? current.getContent() : "";
        }

        // 如果目标版本大于当前版本，666还有穿越者，抛出异常
        if (targetVersion > current.getCurrentVersion()) {
            throw new ServiceException("目标版本不存在");
        }

        // 从当前版本开始逆向应用差异补丁,进行逆向重建
        String content = current.getContent() != null ? current.getContent() : "";
        int currentVer = current.getCurrentVersion();

        // 先处理最近版本列表中的版本（从新到旧逆向应用差异）
        List<WikiPage.RecentVersionInfo> recentVersions = current.getRecentVersions();
        if (recentVersions != null) {
            for (int i = recentVersions.size() - 1; i >= 0 && currentVer > targetVersion; i--) {
                WikiPage.RecentVersionInfo v = recentVersions.get(i);
                if (v.getVersion() != null && v.getVersion() <= currentVer && v.getVersion() > targetVersion) {
                    // 逆向应用差异补丁
                    content = wikiDiffUtils.reversePatch(content, v.getContentDiff());
                    currentVer = v.getVersion() - 1;
                }
            }
        }

        // 如果目标版本更早，需要从历史集合中查询
        if (currentVer > targetVersion) {
            // 查询目标版本与当前重建版本之间的历史记录
            List<WikiVersionHistory> histories = versionHistoryRepository.findByWikiPageIdAndVersionBetween(
                wikiPageId, targetVersion + 1, currentVer
            );

            // 按版本号降序排序并逆向应用
            histories.sort((a, b) -> b.getVersion().compareTo(a.getVersion()));
            for (WikiVersionHistory history : histories) {
                if (history.getVersion() != null && history.getVersion() <= currentVer && history.getVersion() > targetVersion) {
                    content = wikiDiffUtils.reversePatch(content, history.getContentDiff());
                    currentVer = history.getVersion() - 1;
                }
            }
        }

        return content;
    }

    /**
     * 获取最近的版本历史列表（最多10个版本）
     * 这些版本存储在主内容表中，未被归档
     *
     * @param wikiPageId Wiki页面ID
     * @return 最近版本历史列表
     */
    @Override
    public List<WikiPage.RecentVersionInfo> getRecentVersions(Long wikiPageId) {
        WikiPage page = wikiPageRepository.findById(wikiPageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));
        return page.getRecentVersions() != null ? page.getRecentVersions() : new ArrayList<>();
    }

    /**
     * 获取所有版本历史（包括最近版本和已归档版本）
     *
     * @param wikiPageId Wiki页面ID
     * @return 所有版本历史的DTO列表
     */
    @Override
    public List<WikiVersionDTO> getAllVersionHistory(Long wikiPageId) {
        List<WikiVersionDTO> versions = new ArrayList<>();

        // 获取最近版本
        WikiPage wikiPage = wikiPageRepository.findById(wikiPageId).orElse(null);
        if(wikiPage != null && wikiPage.getRecentVersions() != null){
            for (WikiPage.RecentVersionInfo lpf : wikiPage.getRecentVersions()) {
                versions.add(WikiVersionDTO.builder()
                        .version(lpf.getVersion())
                        .changeDescription(lpf.getChangeDescription())
                        .editorId(lpf.getEditorId() != null ? String.valueOf(lpf.getEditorId()) : null)
                        .createdAt(lpf.getCreatedAt())
                        .addedLines(lpf.getAddedLines())
                        .deletedLines(lpf.getDeletedLines())
                        .changedChars(lpf.getChangedChars())
                        .isArchived(false)
                        .build());
            }
        }

        // 获取归档版本
        List<WikiVersionHistory> histories = versionHistoryRepository.findByWikiPageIdOrderByVersionDesc(wikiPageId);
        for (WikiVersionHistory bbbird : histories) {
            versions.add(WikiVersionDTO.builder()
                    .version(bbbird.getVersion())
                    .changeDescription(bbbird.getChangeDescription())
                    .editorId(bbbird.getCreatedBy() != null ? String.valueOf(bbbird.getCreatedBy()) : null)
                    .createdAt(bbbird.getCreatedAt())
                    .addedLines(bbbird.getAddedLines())
                    .deletedLines(bbbird.getDeletedLines())
                    .changedChars(bbbird.getChangedChars())
                    .isArchived(true)
                    .build());
        }

        // 按版本号降序排序
        versions.sort((a, b) -> b.getVersion().compareTo(a.getVersion()));

        return versions;
    }

    /**
     * 比较两个版本之间的内容差异
     *
     * @param wikiPageId Wiki页面ID
     * @param version1   第一个版本号
     * @param version2   第二个版本号
     * @return 两个版本的差异文本
     */
    @Override
    public String compareVersions(Long wikiPageId, Integer version1, Integer version2) {
        // 获取两个版本的完整内容
        String content1 = getVersionContent(wikiPageId, version1);
        String content2 = getVersionContent(wikiPageId, version2);
        // 计算并返回差异
        return wikiDiffUtils.calculateDiff(content1, content2);
    }

    /**
     * 获取版本历史列表（包含详细信息）
     *
     * @param wikiPageId Wiki页面ID
     * @return 版本历史DTO列表
     */
    @Override
    public List<WikiVersionDTO> getVersionHistory(Long wikiPageId) {
        return getAllVersionHistory(wikiPageId);
    }

    /**
     * 删除指定Wiki页面的所有版本历史
     *
     * @param wikiPageId Wiki页面ID
     */
    @Override
    @Transactional
    public void deleteVersionHistory(Long wikiPageId) {
        versionHistoryRepository.deleteByWikiPageId(wikiPageId);
        log.info("删除Wiki版本历史成功: wikiPageId={}", wikiPageId);
    }

    /**
     * 批量删除指定项目下的所有版本历史
     * 用于项目删除时的级联操作
     *
     * @param projectId 项目ID
     */
    @Override
    @Transactional
    public void deleteByProjectId(Long projectId) {
        versionHistoryRepository.deleteByProjectId(projectId);
        log.info("删除项目Wiki版本历史成功: projectId={}", projectId);
    }

    /**
     * 获取版本差异对比—快捷方法
     *
     * @param wikiPageId Wiki页面ID
     * @param version1   版本1
     * @param version2   版本2
     * @return 差异文本
     */
    public String getVersionDiff(Long wikiPageId, Integer version1, Integer version2) {
        String content1 = getVersionContent(wikiPageId, version1);
        String content2 = getVersionContent(wikiPageId, version2);
        return wikiDiffUtils.calculateDiff(content1, content2);
    }

}
