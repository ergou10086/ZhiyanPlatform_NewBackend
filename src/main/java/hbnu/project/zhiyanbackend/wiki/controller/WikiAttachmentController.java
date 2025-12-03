package hbnu.project.zhiyanbackend.wiki.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import hbnu.project.zhiyanbackend.projects.utils.ProjectSecurityUtils;
import hbnu.project.zhiyanbackend.security.utils.PermissionUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.wiki.model.convert.WikiAttachmentMapper;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiAttachmentDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiAttachmentUploadDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiAttachment;
import hbnu.project.zhiyanbackend.wiki.repository.WikiAttachmentRepository;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiOssService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Wiki附件管理控制器
 * 提供Wiki页面的附件上传、下载、删除等功能
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/wiki/attachments")
@RequiredArgsConstructor
@Tag(name = "Wiki附件管理", description = "Wiki附件上传、下载、删除等接口")
public class WikiAttachmentController {

    private final WikiOssService wikiOssService;

    private final WikiPageRepository wikiPageRepository;

    private final WikiAttachmentRepository wikiAttachmentRepository;

    private final ProjectSecurityUtils projectSecurityUtils;

    private final WikiAttachmentMapper wikiAttachmentMapper;

    /**
     * 上传单个附件
     * 权限要求：已登录 + 项目成员
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传附件", description = "上传Wiki页面的图片或文件附件")
    public R<WikiAttachmentDTO> uploadAttachment(
            @Parameter(description = "上传的附件文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Wiki页面ID") @RequestParam("wikiPageId") Long wikiPageId,
            @Parameter(description = "项目ID") @RequestParam("projectId") Long projectId,
            @Parameter(description = "附件类型（IMAGE/FILE，可选）") @RequestParam(value = "attachmentType", required = false) String attachmentType,
            @Parameter(description = "文件描述") @RequestParam(value = "description", required = false) String description) {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]上传Wiki附件: wikiPageId={}, projectId={}, fileName={}",
                userId, wikiPageId, projectId, file.getOriginalFilename());

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权上传附件");
        }

        // 验证wiki页面是否存在并且是否属于该项目
        wikiPageRepository.findByIdAndProjectId(wikiPageId, projectId) .orElseThrow(() -> new ServiceException("Wiki页面不存在或不属于该项目"));

        // 构建上传DTO
        WikiAttachmentUploadDTO uploadDTO = WikiAttachmentUploadDTO.builder()
                .wikiPageId(wikiPageId)
                .projectId(projectId)
                .attachmentType(attachmentType)
                .description(description)
                .uploadBy(userId)
                .build();

        WikiAttachmentDTO result = wikiOssService.uploadAttachment(file, uploadDTO);

        return R.ok(result, "附件上传成功");
    }

    /**
     * 批量上传附件
     * 权限要求：已登录 + 项目成员 + Wiki编辑权限
     */
    @PostMapping("/upload/batch")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "批量上传附件", description = "一次上传多个附件")
    public R<List<WikiAttachmentDTO>> uploadAttachments(
            @Parameter(description = "上传的文件列表") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "Wiki页面ID") @RequestParam("wikiPageId") Long wikiPageId,
            @Parameter(description = "项目ID") @RequestParam("projectId") Long projectId,
            @Parameter(description = "附件类型（IMAGE/FILE，可选）") @RequestParam(value = "attachmentType", required = false) String attachmentType) {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]批量上传Wiki附件: wikiPageId={}, projectId={}, count={}",
                userId, wikiPageId, projectId, files.length);

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权上传附件");
        }

        // 验证Wiki页面是否存在且属于该项目
        wikiPageRepository.findByIdAndProjectId(wikiPageId, projectId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在或不属于该项目"));

        WikiAttachmentUploadDTO uploadDTO = WikiAttachmentUploadDTO.builder()
                .wikiPageId(wikiPageId)
                .projectId(projectId)
                .attachmentType(attachmentType)
                .uploadBy(userId)
                .build();

        List<WikiAttachmentDTO> results = wikiOssService.uploadAttachments(files, uploadDTO);

        return R.ok(results, "批量上传成功");
    }

    /**
     * 获取Wiki页面的所有附件
     * 权限要求：已登录 + 有访问权限
     * 方便前端展示一个页面的所有附件桶
     */
    @GetMapping("/page/{wikiPageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取页面附件", description = "获取Wiki页面的所有附件列表")
    public R<List<WikiAttachmentDTO>> getPageAttachments(@PathVariable Long wikiPageId) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        // 权限检查：获取项目ID并检查访问权限
        Long projectId = wikiPageRepository.findProjectIdById(wikiPageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        List<WikiAttachmentDTO> attachments = wikiOssService.getPageAttachments(wikiPageId);

        return R.ok(attachments);
    }

    /**
     * 获取一个项目中所有wiki页面的附件
     * 分页
     * 权限要求：已登录 + 有访问权限
     * 前端可以切换查看该页面和整个项目的wiki附件桶
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "查询项目附件", description = "分页查询项目的所有附件")
    public R<Page<WikiAttachmentDTO>> queryAttachments(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long wikiPageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        log.debug("用户[{}]查询项目[{}]的附件", userId, projectId);

        // 必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        // 构建分页参数
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<WikiAttachment> attachmentPage = wikiAttachmentRepository.findByProjectIdAndIsDeletedFalse(projectId, pageable);

        // 转换为DTO
        Page<WikiAttachmentDTO> resultPage = attachmentPage.map(attachment -> {
            WikiAttachmentDTO dto = wikiAttachmentMapper.toDTO(attachment);
            return dto;
        });

        return R.ok(resultPage);
    }

    /**
     * 获取Wiki页面的图片列表
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/page/{wikiPageId}/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取页面图片", description = "获取Wiki页面的所有图片")
    public R<List<WikiAttachmentDTO>> getPageImages(@PathVariable Long wikiPageId) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        log.debug("用户[{}]查询Wiki页面[{}]的图片", userId, wikiPageId);

        // 权限检查
        Long projectId = wikiPageRepository.findProjectIdById(wikiPageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        List<WikiAttachmentDTO> images = wikiOssService.getPageImages(wikiPageId);

        return R.ok(images);
    }

    /**
     * 获取Wiki页面的文件列表
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/page/{wikiPageId}/files")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取页面文件", description = "获取Wiki页面的所有文件附件")
    public R<List<WikiAttachmentDTO>> getPageFiles(@PathVariable Long wikiPageId) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        log.debug("用户[{}]查询Wiki页面[{}]的文件", userId, wikiPageId);

        // 权限检查
        Long projectId = wikiPageRepository.findProjectIdById(wikiPageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        List<WikiAttachmentDTO> files = wikiOssService.getPageFiles(wikiPageId);

        return R.ok(files);
    }

    /**
     * 获取附件详情
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取附件详情", description = "获取指定附件的详细信息")
    public R<WikiAttachmentDTO> getAttachment(@PathVariable Long attachmentId) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        log.debug("用户[{}]查询附件[{}]", userId, attachmentId);

        WikiAttachmentDTO attachment = wikiOssService.getAttachment(attachmentId);

        // 权限检查：必须有访问权限
        Long projectId = Long.parseLong(attachment.getProjectId());
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        return R.ok(attachment);
    }

    /**
     * 下载附件
     * 已登录 + 有访问权限
     */
    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "下载附件", description = "下载指定的附件文件")
    public void downloadAttachment(@PathVariable Long attachmentId, HttpServletResponse response) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        log.info("用户[{}]下载附件[{}]", userId, attachmentId);

        WikiAttachmentDTO attachment = wikiOssService.getAttachment(attachmentId);

        // 必须有访问权限
        Long projectId = Long.parseLong(attachment.getProjectId());
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            throw new ServiceException("您不是该项目的成员，无权下载");
        }

        try {
            String downloadUrl = wikiOssService.generateDownloadUrl(attachmentId, 60);
            // 直接重定向到下载URL
            response.sendRedirect(downloadUrl);
            log.info("附件下载成功: attachmentId={}, fileName={}", attachmentId, attachment.getFileName());
        }catch (ControllerException | IOException e){
            log.error("附件下载失败: attachmentId={}", attachmentId, e);
            throw new ServiceException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除附件（软删除）
     * 权限要求：已登录 + 有编辑权限
     */
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除附件", description = "删除指定的附件（软删除）")
    public R<Void> deleteAttachment(@PathVariable Long attachmentId) {
        Long userId = SecurityUtils.getUserId();
        ValidationUtils.requireNonNull(userId, "未登录或令牌无效");
        log.info("用户[{}]删除附件[{}]", userId, attachmentId);

        WikiAttachmentDTO attachment = wikiOssService.getAttachment(attachmentId);

        // 权限检查：必须有编辑权限
        Long projectId = Long.parseLong(attachment.getProjectId());
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权删除");
        }

        wikiOssService.deleteAttachment(attachmentId, userId);

        return R.ok(null, "附件删除成功");
    }

    /**
     * 物理删除附件
     * 权限要求：已登录 + 项目成员
     * 目前先直接使用这个
     */
    @DeleteMapping("/{attachmentId}/permanent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "物理删除附件", description = "从对象存储和数据库中彻底删除附件")
    public R<Void> deleteAttachmentPermanently(@PathVariable Long attachmentId) {
        Long userId = SecurityUtils.getUserId();
        log.info("用户[{}]删除附件[{}]", userId, attachmentId);

        WikiAttachmentDTO attachment = wikiOssService.getAttachment(attachmentId);

        // 权限检查：必须是项目成员
        Long projectId = Long.parseLong(attachment.getProjectId());
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权删除");
        }

        wikiOssService.deleteAttachmentPermanently(attachmentId);

        return R.ok(null, "附件已彻底删除");
    }

    /**
     * 批量删除Wiki页面的所有附件（物理删除）
     * 权限要求：已登录 + 有删除权限
     * 该接口修改为项目内谁都能删除。抛弃冗余管理员职责
     */
    @DeleteMapping("/page/{wikiPageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除页面所有附件", description = "物理删除Wiki页面的所有附件（不可恢复）")
    public R<Void> deletePageAttachments(@PathVariable Long wikiPageId) {
        Long userId = SecurityUtils.getUserId();
        log.info("用户[{}]删除附件[{}]", userId, wikiPageId);

        // 权限检查：获取项目ID并检查删除权限
        Long projectId = wikiPageRepository.findProjectIdById(wikiPageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权删除");
        }

        wikiOssService.deletePageAttachments(wikiPageId);

        return R.ok(null, "页面附件已全部彻底删除");
    }

    /**
     * 获取项目附件统计信息
     * 权限要求：已登录 + 项目成员
     */
    @GetMapping("/project/{projectId}/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取附件统计", description = "获取项目的附件统计信息")
    public R<Map<String, Object>> getAttachmentStats(@PathVariable Long projectId) {
        Long userId = SecurityUtils.getUserId();
        log.debug("用户[{}]查询项目[{}]的附件统计", userId, projectId);

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权访问");
        }

        Map<String, Object> stats = wikiOssService.getProjectAttachmentStats(projectId);

        return R.ok(stats);
    }
}
