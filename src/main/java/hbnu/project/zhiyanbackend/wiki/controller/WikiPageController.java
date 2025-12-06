package hbnu.project.zhiyanbackend.wiki.controller;

import hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog;
import hbnu.project.zhiyanbackend.activelog.core.OperationLogHelper;
import hbnu.project.zhiyanbackend.activelog.model.enums.BizOperationModule;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import hbnu.project.zhiyanbackend.projects.utils.ProjectSecurityUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.wiki.model.dto.*;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wiki页面控制器
 * 提供Wiki页面的CRUD和权限控制
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/wiki")
@RequiredArgsConstructor
@Tag(name = "Wiki页面管理", description = "Wiki页面管理相关接口")
public class WikiPageController {

    private final WikiPageService wikiPageService;

    private final ProjectSecurityUtils projectSecurityUtils;

    private final WikiPageRepository wikiPageRepository;
    
    private final OperationLogHelper operationLogHelper;

    /**
     * 创建Wiki页面
     * 权限要求：已登录 + 项目成员
     */
    @PostMapping("/pages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建Wiki页面", description = "创建新的Wiki页面（目录或文档）")
    @BizOperationLog(module = BizOperationModule.WIKI, type = "CREATE", description = "创建Wiki页面")
    public R<WikiPage> createPage(@RequestBody @Valid CreateWikiPageDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        Long projectId = dto.getProjectId();
        log.info("用户[{}]创建Wiki页面: projectId={}, title={}", userId, projectId, dto.getTitle());
        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权上传附件");
        }

        dto.setCreatorId(userId);
        WikiPage page = wikiPageService.createWikiPage(dto);

        // 记录创建Wiki页面日志
        if (page != null && page.getId() != null) {
            operationLogHelper.logWikiCreate(projectId, page.getId(), page.getTitle());
        }

        return R.ok(page, "Wiki页面创建成功");
    }

    /**
     * 更新Wiki页面
     * 权限要求：已登录 + 项目成员
     */
    @PutMapping("/pages/{pageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新Wiki页面", description = "更新Wiki页面的标题和内容")
    @BizOperationLog(module = BizOperationModule.WIKI, type = "UPDATE", description = "更新Wiki页面")
    public R<WikiPage> updatePage(
            @Parameter(description = "页面ID") @PathVariable Long pageId,
            @RequestBody @Valid UpdateWikiPageDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.info("用户[{}]更新Wiki页面[{}]", userId, pageId);

        // 权限检查：必须是项目成员且有编辑权限
        if (!projectSecurityUtils.isWikiPageMember(pageId, userId)) {
            return R.fail("您不是该项目的成员，无权编辑");
        }

        if (!projectSecurityUtils.canEditWikiPage(pageId)) {
            return R.fail("您没有编辑该Wiki页面的权限");
        }

        dto.setEditorId(userId);
        WikiPage page = wikiPageService.updateWikiPage(
                pageId,
                dto.getTitle(),
                dto.getContent(),
                dto.getChangeDescription(),
                userId
        );

        // 记录更新Wiki页面日志
        if (page != null) {
            operationLogHelper.logWikiUpdate(page.getProjectId(), pageId, page.getTitle());
        }

        return R.ok(page, "Wiki页面更新成功");
    }

    /**
     * 删除Wiki页面
     * 权限要求：已登录 + 有删除权限（创建者或管理员）
     */
    @DeleteMapping("/pages/{pageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除Wiki页面", description = "删除指定Wiki页面")
    @BizOperationLog(module = BizOperationModule.WIKI, type = "DELETE", description = "删除Wiki页面")
    public R<Void> deletePage(@PathVariable Long pageId) {
        Long userId = SecurityUtils.getUserId();
        log.info("用户[{}]删除Wiki页面[{}]", userId, pageId);

        // 权限检查：必须有删除权限
        if (!projectSecurityUtils.canDeleteWikiPage(pageId, userId)) {
            return R.fail("您没有删除该Wiki页面的权限");
        }

        // 获取页面信息用于日志记录
        WikiPage page = wikiPageRepository.findById(pageId).orElse(null);
        Long projectId = page != null ? page.getProjectId() : null;
        String pageTitle = page != null ? page.getTitle() : null;

        wikiPageService.deleteWikiPage(pageId, userId);

        // 记录删除Wiki页面日志
        if (projectId != null) {
            operationLogHelper.logWikiDelete(projectId, pageId, pageTitle);
        }

        return R.ok(null, "Wiki页面删除成功");
    }

    /**
     * 递归删除wiki页面及其子页面
     * 也就是按照树删掉一整个后序遍历的子树
     * 权限要求：已登录 + 项目成员
     */
    @DeleteMapping("/pages/{pageId}/recursive")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "递归删除Wiki页面", description = "删除Wiki页面及其所有子页面")
    public R<Void> deletePageRecursively(@PathVariable Long pageId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]递归删除Wiki页面[{}]", userId, pageId);

        // 必须有删除权限
        if (!projectSecurityUtils.canDeleteWikiPage(pageId, userId)) {
            return R.fail("您没有删除该Wiki页面的权限");
        }

        wikiPageService.deletePageRecursively(pageId, userId);

        return R.ok(null, "Wiki页面及子页面删除成功");
    }

    /**
     * 获取Wiki页面详情（含内容）
     * 权限要求：已登录 + 有访问权限（项目成员或公开页面）
     */
    @GetMapping("/pages/{pageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取Wiki页面详情", description = "获取Wiki页面的完整信息和内容")
    public R<WikiPageDetailDTO> getPageDetail(@PathVariable Long pageId) {
        Long userId = SecurityUtils.getUserId();
        log.debug("用户[{}]查询Wiki页面[{}]", userId, pageId);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        WikiPageDetailDTO detail = wikiPageService.getWikiPageWithContent(pageId);

        return R.ok(detail);
    }

    /**
     * 获取项目的Wiki树状结构
     * 权限要求：已登录 + 项目成员
     */
    @GetMapping("/projects/{projectId}/tree")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取Wiki树", description = "获取项目的Wiki树状目录结构")
    public R<List<WikiPageTreeDTO>> getProjectWikiTree(@PathVariable Long projectId) {
        Long userId = SecurityUtils.getUserId();
        log.debug("用户[{}]查询项目[{}]的Wiki树", userId, projectId);

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        List<WikiPageTreeDTO> tree = wikiPageService.getProjectWikiTree(projectId);

        return R.ok(tree);
    }

    /**
     * 搜索Wiki页面（标题）
     * 权限要求：已登录 + 项目成员
     */
    @GetMapping("/projects/{projectId}/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "搜索Wiki页面", description = "根据关键字搜索项目中的Wiki页面")
    public R<Page<WikiSearchDTO>> searchWiki(
            @PathVariable Long projectId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]搜索项目[{}]的Wiki标题: keyword={}, page={}, size={}", userId, projectId, keyword, page, size);

        // 参数验证
        ValidationUtils.requireNonNull(keyword, "keyword不能为空");
        ValidationUtils.requireNonBlank(keyword, "keyword必须要有内容");
        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权搜索");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<WikiSearchDTO> result = wikiPageService.searchByTitle(projectId, keyword.trim(), pageable);

        return R.ok(result);
    }

    /**
     * 全文搜索Wiki内容
     * 权限要求：已登录 + 项目成员
     */
    @GetMapping("/projects/{projectId}/search/content")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "全文搜索Wiki", description = "根据关键字搜索Wiki内容（PostgreSQL全文搜索）")
    public R<Page<WikiSearchDTO>> searchContent(
            @PathVariable Long projectId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]全文搜索项目[{}]的Wiki: keyword={}, page={}, size={}", userId, projectId, keyword, page, size);

        // 参数验证
        ValidationUtils.requireNonNull(keyword, "keyword搜索关键字不能为空");
        ValidationUtils.requireNonBlank(keyword, "keyword搜索关键字必须要有内容");

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权搜索");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<WikiSearchDTO> result = wikiPageService.searchByContent(projectId, keyword.trim(), pageable);

        return R.ok(result);
    }

    /**
     * 全文搜索Wiki内容（整合搜索，同时搜索标题和内容）
     * 权限要求：已登录 + 项目成员
     */
    @GetMapping("/projects/{projectId}/search/fulltext")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "全文搜索Wiki", description = "基于PostgreSQL全文搜索，同时搜索标题和内容")
    public R<Page<WikiSearchDTO>> fullTextSearch(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "搜索关键字") @RequestParam String keyword,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.info("用户[{}]全文搜索项目[{}]: keyword={}, page={}, size={}",
                userId, projectId, keyword, page, size);

        // 参数验证
        if (keyword == null || keyword.trim().isEmpty()) {
            return R.fail("搜索关键字不能为空");
        }
        if (keyword.length() > 200) {
            return R.fail("搜索关键字过长，最多200个字符");
        }
        if (page < 0) {
            return R.fail("页码必须大于等于0");
        }
        if (size <= 0 || size > 100) {
            return R.fail("每页数量必须在1-100之间");
        }

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权搜索");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<WikiSearchDTO> results = wikiPageService.fullTextSearch(projectId, keyword.trim(), pageable);

        return R.ok(results);
    }

    /**
     * 移动Wiki页面
     * 权限要求：已登录 + 有编辑权限
     */
    @PatchMapping("/pages/{pageId}/move")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "移动Wiki页面", description = "移动Wiki页面到新的父页面下")
    @BizOperationLog(module = BizOperationModule.WIKI, type = "MOVE", description = "移动Wiki页面")
    public R<Void> movePage(
            @PathVariable Long pageId,
            @RequestBody @Valid MoveWikiPageDTO dto) {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.info("用户[{}]移动Wiki页面[{}]到父页面[{}]", userId, pageId, dto.getNewParentId());

        // 权限检查：必须有编辑权限
        if (!projectSecurityUtils.canEditWikiPage(pageId)) {
            return R.fail("您没有编辑该Wiki页面的权限");
        }

        // 获取页面信息用于日志记录
        WikiPage page = wikiPageRepository.findById(pageId).orElse(null);
        Long projectId = page != null ? page.getProjectId() : null;
        String pageTitle = page != null ? page.getTitle() : null;

        wikiPageService.moveWikiPage(pageId, dto.getNewParentId(), userId);

        // 记录移动Wiki页面日志
        if (projectId != null) {
            operationLogHelper.logWikiMove(projectId, pageId, pageTitle, dto.getNewParentId());
        }

        return R.ok(null, "Wiki页面移动成功");
    }

    /**
     * 复制Wiki页面
     * 权限要求：已登录 + 项目成员
     */
    @PostMapping("/pages/{pageId}/copy")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "复制Wiki页面", description = "复制Wiki页面到指定位置")
    public R<WikiPage> copyPage(
            @PathVariable Long pageId,
            @RequestParam(required = false) Long targetParentId,
            @RequestParam(required = false) String newTitle) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]复制Wiki页面[{}]", userId, pageId);

        // 权限检查：必须有访问权限和创建权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            return R.fail("您没有权限访问此Wiki页面");
        }

        // 获取源页面以获取项目ID
        WikiPage sourcePage = wikiPageRepository.findById(pageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        // 检查目标项目的成员权限
        if (targetParentId != null) {
            WikiPage targetParent = wikiPageRepository.findById(targetParentId)
                    .orElseThrow(() -> new ServiceException("目标父页面不存在"));

            if (!projectSecurityUtils.isMember(targetParent.getProjectId(), userId)) {
                return R.fail("您不是目标项目的成员，无权复制到此位置");
            }
        }else {
            // 复制到根目录，需要检查源项目的成员权限,虽然感觉不需要特别区分
            if (!projectSecurityUtils.isMember(sourcePage.getProjectId(), userId)) {
                return R.fail("您不是该项目的成员，无权复制");
            }
        }

        WikiPage newPage = wikiPageService.copyPage(pageId, targetParentId, newTitle, userId);

        return R.ok(newPage, "Wiki页面复制成功");
    }

    /**
     * 获取项目Wiki统计信息
     * 权限要求：已登录 + 项目成员
     */
    @GetMapping("/projects/{projectId}/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取Wiki统计", description = "获取项目Wiki的统计信息")
    public R<WikiStatisticsDTO> getStatistics(@PathVariable Long projectId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.debug("用户[{}]查询项目[{}]的Wiki统计", userId, projectId);

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        WikiStatisticsDTO statistics = wikiPageService.getProjectStatistics(projectId);

        return R.ok(statistics);
    }

    /**
     * 获取最近更新的Wiki页面
     * 权限要求：已登录 + 项目成员
     */
    @GetMapping("/projects/{projectId}/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取最近更新", description = "获取项目中最近更新的Wiki页面")
    public R<List<WikiPageTreeDTO>> getRecentUpdateWikiPage(@PathVariable Long projectId,
                                                            @RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        log.debug("用户[{}]查询项目[{}]最近更新的Wiki", userId, projectId);

        // 权限检查
        if(!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        List<WikiPageTreeDTO> pages = wikiPageService.getRecentlyUpdated(projectId, limit);

        return R.ok(pages);
    }
}
