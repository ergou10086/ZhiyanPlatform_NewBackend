package hbnu.project.zhiyanbackend.wiki.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.projects.utils.ProjectSecurityUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiVersionDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiContentVersionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wiki内容控制器
 * 提供Wiki内容版本管理的相关接口
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/wiki/content")     // 原 /api/wiki/content
@RequiredArgsConstructor
@Tag(name = "Wiki内容管理", description = "Wiki内容版本管理相关接口")
public class WikiContentController {

    private final WikiContentVersionService contentVersionService;
    private final WikiPageRepository wikiPageRepository;
    private final ProjectSecurityUtils projectSecurityUtils;

    /**
     * 获取Wiki页面的版本历史列表
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/versions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取版本历史", description = "获取Wiki页面的所有版本历史记录")
    public R<List<WikiVersionDTO>> getVersionHistory(@PathVariable Long pageId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.debug("用户[{}]查询Wiki页面[{}]的版本历史", userId, pageId);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        List<WikiVersionDTO> versions = contentVersionService.getVersionHistory(pageId);

        return R.ok(versions);
    }

    /**
     * 获取指定版本的内容
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/versions/{version}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取指定版本内容", description = "获取Wiki页面指定版本的完整内容")
    public R<String> getVersionContent(
            @Parameter(description = "页面ID") @PathVariable Long pageId,
            @Parameter(description = "版本号") @PathVariable Integer version) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.debug("用户[{}]查询Wiki页面[{}]的版本[{}]", userId, pageId, version);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        String content = contentVersionService.getVersionContent(pageId, version);

        return R.ok(content);
    }

    /**
     * 比较两个版本之间的差异
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/compare")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "比较版本差异", description = "比较Wiki页面两个版本之间的内容差异")
    public R<String> compareVersions(
            @Parameter(description = "页面ID") @PathVariable Long pageId,
            @Parameter(description = "版本1") @RequestParam Integer version1,
            @Parameter(description = "版本2") @RequestParam Integer version2) {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.debug("用户[{}]比较Wiki页面[{}]的版本差异: v{} vs v{}", userId, pageId, version1, version2);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        String diff = contentVersionService.compareVersions(pageId, version1, version2);

        return R.ok(diff);
    }

    /**
     * 获取Wiki页面当前内容
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前内容", description = "获取Wiki页面当前版本的内容")
    public R<String> getCurrentContent(@PathVariable Long pageId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.debug("用户[{}]查询Wiki页面[{}]的当前内容", userId, pageId);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        // 先查询当前页面的内容
        WikiPage wikiPage =  wikiPageRepository.findById(pageId).orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        if(wikiPage.getPageType() == PageType.DIRECTORY){
            return R.ok("", "目录类型的页面没有内容");
        }

        // 获取当前版本内容
        String content = contentVersionService.getVersionContent(pageId, wikiPage.getCurrentVersion());

        return R.ok(content);
    }

    /**
     * 获取最近的版本历史（最多10个）
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/versions/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取最近版本", description = "获取Wiki页面最近的版本历史（最多10个）")
    public R<List<WikiPage.RecentVersionInfo>> getRecentVersions(@PathVariable Long pageId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.debug("用户[{}]查询Wiki页面[{}]的最近版本", userId, pageId);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        List<WikiPage.RecentVersionInfo> wikiVersions = contentVersionService.getRecentVersions(pageId);

        return R.ok(wikiVersions);
    }

    /**
     * 获取所有版本历史（包括归档的）
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/versions/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取所有版本", description = "获取Wiki页面的所有版本历史（包括已归档的）")
    public R<List<WikiVersionDTO>> getAllVersionHistory(@PathVariable Long pageId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.debug("用户[{}]查询Wiki页面[{}]的所有版本历史", userId, pageId);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        List<WikiVersionDTO> versions = contentVersionService.getAllVersionHistory(pageId);

        return R.ok(versions);
    }
}
