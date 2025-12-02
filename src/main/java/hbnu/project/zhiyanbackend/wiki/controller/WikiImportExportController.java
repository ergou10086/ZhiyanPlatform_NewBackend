package hbnu.project.zhiyanbackend.wiki.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.utils.ProjectSecurityUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiExportDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiImportDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiImportResultDTO;
import hbnu.project.zhiyanbackend.wiki.model.enums.ExportFormat;
import hbnu.project.zhiyanbackend.wiki.service.WikiIMEXportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static hbnu.project.zhiyanbackend.basic.utils.FileUtils.getFileExtension;

/**
 * Wiki导入导出控制器
 * 提供Wiki页面的导入导出功能
 *
 * @author Tokito
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/wiki")     // 原 /api/wiki
@RequiredArgsConstructor
@Tag(name = "Wiki导入导出", description = "Wiki页面导入导出相关接口")
public class WikiImportExportController {

    private final WikiIMEXportService wikiIMEXportService;
    private final ProjectSecurityUtils projectSecurityUtils;

    /**
     * 导出单个Wiki页面
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "导出Wiki页面", description = "将Wiki页面导出为指定格式（Markdown/PDF/Word）")
    public void exportPage(
            @Parameter(description = "导出的页面id") @PathVariable Long pageId,
            @Parameter(description = "导出格式，默认md") @RequestParam(defaultValue = "MARKDOWN") String format,
            @Parameter(description = "是否包括子页面") @RequestParam(defaultValue = "false") Boolean includeChildren,
            @Parameter(description = "是否连着附件一起导出") @RequestParam(defaultValue = "false") Boolean includeAttachments,
            HttpServletResponse response) throws IOException {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或令牌无效");
            return;
        }
        log.info("用户[{}]导出Wiki页面[{}]，格式: {}", userId, pageId, format);
        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "您没有权限访问此Wiki页面");
            return;
        }

        // 构建导出配置
        WikiExportDTO exportDTO = WikiExportDTO.builder()
                .format(format)
                .includeChildren(includeChildren)
                .includeAttachments(includeAttachments)
                .build();

        // 导出页面
        byte[] content = wikiIMEXportService.exportPage(pageId, exportDTO);

        // 设置响应头
        String fileName = "wiki_page_" + pageId + getFileExtension(format);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType(getContentType(format));
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        response.setContentLength(content.length);

        // 写入响应
        response.getOutputStream().write(content);
        response.flushBuffer();

        log.info("Wiki页面导出成功: pageId={}, format={}, size={}", pageId, format, content.length);
    }

    /**
     * 批量导出Wiki页面（打包为ZIP）
     * 权限要求：已登录 + 项目成员
     */
    @PostMapping("/projects/{projectId}/export/batch")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "批量导出Wiki页面", description = "将多个Wiki页面打包导出为ZIP文件")
    public void exportPages(
            @Parameter(description = "项目id") @PathVariable Long projectId,
            @Parameter(description = "批量导出的页面id") @RequestBody List<Long> pageIds,
            @Parameter(description = "页面描述") @RequestParam(defaultValue = "MARKDOWN") String format,
            HttpServletResponse response) throws IOException {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或令牌无效");
            return;
        }
        log.info("用户[{}]批量导出项目[{}]的Wiki页面，数量: {}", userId, projectId, pageIds.size());

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "您不是该项目的成员，无权导出");
            return;
        }

        // 构建导出配置
        WikiExportDTO exportDTO = WikiExportDTO.builder()
                .format(format)
                .pageIds(pageIds)
                .build();

        // 批量导出
        byte[] zipContent = wikiIMEXportService.exportPages(pageIds, exportDTO);

        // 设置响应头
        String fileName = "wiki_pages_" + projectId + ".zip";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType(getContentType(format));
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,  "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        response.setContentLength(zipContent.length);

        // 写入响应中
        response.getOutputStream().write(zipContent);
        response.flushBuffer();

        log.info("批量导出成功: projectId={}, count={}, size={}", projectId, pageIds.size(), zipContent.length);
    }

    /**
     * 导出整个Wiki目录树
     * 权限要求：已登录 + 有访问权限
     */
    @GetMapping("/pages/{pageId}/export/directory")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "导出Wiki目录树", description = "导出Wiki目录及其所有子页面（ZIP格式）")
    public void exportDirectory(
            @Parameter(description = "页面id") @PathVariable Long pageId,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "MARKDOWN") String format,
            HttpServletResponse response) throws IOException {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或令牌无效");
            return;
        }

        log.info("用户[{}]导出Wiki目录树[{}]", userId, pageId);

        // 权限检查：必须有访问权限
        if (!projectSecurityUtils.canAccessWikiPage(pageId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "您没有权限访问此Wiki页面");
            return;
        }

        // 构建导出配置
        WikiExportDTO exportDTO = WikiExportDTO.builder()
                .format(format)
                .includeChildren(true)
                .build();

        // 导出目录树
        byte[] zipContent = wikiIMEXportService.exportDirectory(pageId, exportDTO);

        // 设置响应头
        String fileName = "wiki_directory_" + pageId + ".zip";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        response.setContentLength(zipContent.length);

        // 写入响应
        response.getOutputStream().write(zipContent);
        response.flushBuffer();

        log.info("目录树导出成功: pageId={}, size={}", pageId, zipContent.length);
    }

    /**
     * 导入Markdown文件
     * 权限要求：已登录 + 项目成员
     */
    @PostMapping("/projects/{projectId}/import")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "导入Markdown文件", description = "从Markdown文件创建Wiki页面")
    public R<WikiImportResultDTO> importMarkdown(
            @Parameter(description = "项目id") @PathVariable Long projectId,
            @Parameter(description = "Markdown文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "父页面ID（可选）") @RequestParam(required = false) Long parentId,
            @Parameter(description = "是否覆盖同名页面") @RequestParam(defaultValue = "false") Boolean overwrite,
            @Parameter(description = "是否设为公开") @RequestParam(defaultValue = "false") Boolean isPublic) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("用户[{}]导入Markdown到项目[{}]，文件: {}", userId, projectId, file.getOriginalFilename());

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权导入");
        }

        // 构建导入配置
        WikiImportDTO importDTO = WikiImportDTO.builder()
                .projectId(projectId)
                .parentId(parentId)
                .format("MARKDOWN")
                .overwrite(overwrite)
                .isPublic(isPublic)
                .importBy(userId)
                .build();

        // 执行导入
        WikiImportResultDTO result = wikiIMEXportService.importFromMarkdown(file, importDTO);

        if (result.getSuccess()) {
            return R.ok(result, "导入成功");
        } else {
            return R.fail(result, result.getMessage());
        }
    }

    /**
     * 批量导入Markdown文件
     * 权限要求：已登录 + 项目成员
     */
    @PostMapping("/projects/{projectId}/import/batch")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "批量导入Markdown文件", description = "一次导入多个Markdown文件")
    public R<WikiImportResultDTO> importMultipleMarkdown(
            @PathVariable Long projectId,
            @Parameter(description = "Markdown文件列表") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "父页面ID（可选）") @RequestParam(required = false) Long parentId,
            @Parameter(description = "是否覆盖同名页面") @RequestParam(defaultValue = "false") Boolean overwrite,
            @Parameter(description = "是否设为公开") @RequestParam(defaultValue = "false") Boolean isPublic) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.info("用户[{}]批量导入Markdown到项目[{}]，文件数: {}", userId, projectId, files.length);

        // 权限检查：必须是项目成员
        if (!projectSecurityUtils.isMember(projectId, userId)) {
            return R.fail("您不是该项目的成员，无权导入");
        }

        // 构建导入配置
        WikiImportDTO importDTO = WikiImportDTO.builder()
                .projectId(projectId)
                .parentId(parentId)
                .format("MARKDOWN")
                .overwrite(overwrite)
                .isPublic(isPublic)
                .importBy(userId)
                .build();

        // 执行批量导入
        WikiImportResultDTO result = wikiIMEXportService.importMultipleMarkdown(files, importDTO);

        return R.ok(result, result.getMessage());
    }

    /**
     * 获取Content-Type
     */
    private String getContentType(String format) {
        try {
            ExportFormat exportFormat = ExportFormat.valueOf(format.toUpperCase());
            return exportFormat.getMimeType() + "; charset=UTF-8";
        } catch (IllegalArgumentException e) {
            // 如果格式不匹配，返回默认Content-Type
            return switch (format.toUpperCase()) {
                case "MARKDOWN", "MD" -> "text/markdown; charset=UTF-8";
                case "PDF" -> "application/pdf";
                case "WORD", "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
            };
        }
    }
}

