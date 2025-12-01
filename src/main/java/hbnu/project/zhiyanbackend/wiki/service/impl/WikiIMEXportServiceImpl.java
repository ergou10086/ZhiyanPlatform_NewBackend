package hbnu.project.zhiyanbackend.wiki.service.impl;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.DateUtils;
import hbnu.project.zhiyanbackend.basic.utils.FileUtils;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import hbnu.project.zhiyanbackend.wiki.model.dto.CreateWikiPageDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiExportDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiImportDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiImportResultDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.model.enums.ExportFormat;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiIMEXportService;
import hbnu.project.zhiyanbackend.wiki.service.WikiPageService;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Wiki 导入导出服务实现
 * 基于新架构（PostgreSQL + WikiPage 直接存内容）
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiIMEXportServiceImpl implements WikiIMEXportService {

    private final WikiPageRepository wikiPageRepository;
    private final WikiPageService wikiPageService;
    private final ApplicationContext applicationContext;

    // ==================== 导入实现 ====================

    /**
     * 从单个 Markdown 文件导入 Wiki 页面
     *
     * @param file      Markdown 文件
     * @param importDTO 导入配置
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public WikiImportResultDTO importFromMarkdown(MultipartFile file, WikiImportDTO importDTO) {
        // 使用 ValidationUtils 校验参数
        ValidationUtils.requireNonNull(file, "导入文件不能为空");
        ValidationUtils.requireNonNull(importDTO, "导入配置不能为空");
        ValidationUtils.requireNonNull(importDTO.getProjectId(), "项目ID不能为空");
        ValidationUtils.requireNonNull(importDTO.getImportBy(), "导入用户不能为空");

        WikiImportResultDTO result = WikiImportResultDTO.builder()
                .success(false)
                .build();

        try{
            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            ValidationUtils.requireNonBlank(originalFilename, "文件名");

            String fileExtension = FileUtils.getFileExtension(originalFilename);
            if (!"md".equalsIgnoreCase(fileExtension) && !"markdown".equalsIgnoreCase(fileExtension)) {
                throw new ServiceException("只支持Markdown格式文件 (.md, .markdown)");
            }

            // 读取文件内容
            String content = readFileContent(file);

            // 解析Markdown
            MarkdownPage mdPage = parseMarkdown(content);

            // 检查是否存在同名页面
            if (!Boolean.TRUE.equals(importDTO.getOverwrite())) {
                List<WikiPage> existingPages = wikiPageRepository.findByProjectIdAndTitleAndParentId(
                            importDTO.getProjectId(), mdPage.getTitle(), importDTO.getParentId());
                if (!existingPages.isEmpty()) {
                    result.getErrors().add("页面已存在: " + mdPage.getTitle());
                    result.setMessage("导入失败：页面已存在且未设置覆盖模式");
                    return result;
                }
            }

            // 使用 JsonUtils 记录导入配置（用于日志）
            String importConfig = JsonUtils.toJsonString(importDTO);
            log.debug("导入配置: {}", importConfig);

            // 创建Wiki页面（使用新的 WikiPageService）
            CreateWikiPageDTO createDTO = CreateWikiPageDTO.builder()
                    .projectId(importDTO.getProjectId())
                    .parentId(importDTO.getParentId())
                    .title(mdPage.getTitle())
                    .content(mdPage.getContent())
                    .pageType(PageType.DOCUMENT)
                    .isPublic(importDTO.getIsPublic())
                    .creatorId(importDTO.getImportBy())
                    .changeDescription("从Markdown导入")
                    .build();

            WikiPage createdPage = wikiPageService.createWikiPage(createDTO);

            // 设置正确结果
            result.setSuccess(true);
            result.setImportedCount(1);
            result.getPageIds().add(String.valueOf(createdPage.getId()));
            result.setMessage("导入成功：" + mdPage.getTitle());

            log.info("Markdown导入成功: pageId={}, title={}", createdPage.getId(), mdPage.getTitle());
        }catch (ServiceException | IOException e){
            log.error("Markdown导入失败", e);
            // 设置错误结果
            result.setFailedCount(1);
            result.getErrors().add(e.getMessage());
            result.setMessage("导入失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 批量导入多个 Markdown 文件
     *
     * @param files     文件数组
     * @param importDTO 导入配置
     * @return 汇总导入结果
     */
    @Override
    public WikiImportResultDTO importMultipleMarkdown(MultipartFile[] files, WikiImportDTO importDTO) {
        // 参数校验
        ValidationUtils.requireNonNull(files, "文件数组不能为空");
        ValidationUtils.requireNonEmpty(List.of(files), "至少需要一个文件");
        ValidationUtils.requireNonNull(importDTO, "导入配置不能为空");

        WikiImportResultDTO result = WikiImportResultDTO.builder()
                .success(true)
                .importedCount(0)
                .failedCount(0)
                .errors(new ArrayList<>())
                .pageIds(new ArrayList<>())
                .build();

        if (files.length == 0) {
            result.setSuccess(false);
            result.setMessage("没有可导入的文件");
            return result;
        }

        for(MultipartFile file : files){
            try{
                String filename = FileUtils.getSafeFilename(file.getOriginalFilename());

                WikiImportResultDTO singleResult = applicationContext.getBean(WikiIMEXportServiceImpl.class)
                        .importFromMarkdown(file, importDTO);

                if (Boolean.TRUE.equals(singleResult.getSuccess())) {
                    result.setImportedCount(result.getImportedCount() + 1);
                    result.getPageIds().addAll(singleResult.getPageIds());
                } else {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.getErrors().addAll(singleResult.getErrors());
                }
            }catch (Exception e) {
                log.error("导入文件失败: {}", file.getOriginalFilename(), e);
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        // 出现失败就标记
        if (result.getFailedCount() > 0) {
            result.setSuccess(false);
        }

        String message = String.format("批量导入完成：成功 %d 个，失败 %d 个",
                result.getImportedCount(), result.getFailedCount());
        result.setMessage(message);

        log.info("批量导入完成: {}", message);

        // 记录详细结果日志
        if (!result.getErrors().isEmpty()) {
            log.warn("导入错误详情: {}", JsonUtils.toJsonString(result.getErrors()));
        }

        return result;
    }

    // ==================== 导出实现 ====================

    /**
     * 导出单个 Wiki 页面
     *
     * @param pageId    页面ID
     * @param exportDTO 导出配置
     * @return 导出的文件内容（字节数组）
     */
    @Override
    public byte[] exportPage(Long pageId, WikiExportDTO exportDTO) {
        ValidationUtils.requireId(pageId, "页面ID");
        ValidationUtils.requireNonNull(exportDTO, "导出配置");

        WikiPage page = wikiPageRepository.findById(pageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        ExportFormat format = getExportFormat(exportDTO.getFormat());

        byte[] result = switch (format) {
            case MARKDOWN -> exportToMarkdown(page, exportDTO);
            case PDF -> exportToPdf(page, exportDTO);
            case WORD -> exportToWord(page, exportDTO);
        };

        // 记录导出统计
        log.debug("导出完成: 页面={}, 大小={}", page.getTitle(),
                FileUtils.formatFileSize(result.length));

        return result;
    }

    /**
     * 批量导出多个 Wiki 页面（打包为 ZIP）
     *
     * @param pageIds   页面ID列表
     * @param exportDTO 导出配置
     * @return ZIP 文件内容
     */
    @Override
    public byte[] exportPages(List<Long> pageIds, WikiExportDTO exportDTO) {
        // 参数校验
        ValidationUtils.requireNonEmpty(pageIds, "页面ID列表");
        ValidationUtils.requireNonNull(exportDTO, "导出配置");

        // 限制最大导出数量
        final int MAX_EXPORT_COUNT = 100;
        ValidationUtils.assertTrue(pageIds.size() <= MAX_EXPORT_COUNT,
                String.format("一次最多导出 %d 个页面", MAX_EXPORT_COUNT));

        if (pageIds.isEmpty()) {
            throw new ServiceException("导出页面ID列表不能为空");
        }

        try (ByteArrayOutputStream bbbird = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bbbird)) {

            int successCount = 0;
            int skipCount = 0;

            for (Long pageId : pageIds) {
                try {
                    WikiPage page = wikiPageRepository.findById(pageId).orElse(null);
                    if (page == null) {
                        log.warn("页面不存在，跳过: pageId={}", pageId);
                        skipCount++;
                        continue;
                    }

                    // 导出页面
                    byte[] content = exportPage(pageId, exportDTO);
                    // 生成安全的文件名
                    String fileName = generateFileName(page, exportDTO.getFormat());
                    String safeFileName = FileUtils.getSafeFilename(fileName);

                    // 添加到zip
                    ZipEntry entry = new ZipEntry(safeFileName);
                    zos.putNextEntry(entry);
                    zos.write(content);
                    zos.closeEntry();

                    successCount++;
                    log.debug("成功导出: {}", safeFileName);
                }catch (ServiceException e) {
                    log.error("导出页面失败: pageId={}", pageId, e);
                    skipCount++;
                }
            }
            zos.finish();
            return bbbird.toByteArray();
        }catch (IOException e) {
            log.error("创建ZIP文件失败", e);
            throw new ServiceException("批量导出失败: " + e.getMessage());
        }
    }

    /**
     * 导出整个目录树（打包为 ZIP）
     *
     * @param rootPageId 根页面ID（目录）
     * @param exportDTO  导出配置
     * @return ZIP 文件内容
     */
    @Override
    public byte[] exportDirectory(Long rootPageId, WikiExportDTO exportDTO) {
        // 参数校验
        ValidationUtils.requireId(rootPageId, "根页面ID");
        ValidationUtils.requireNonNull(exportDTO, "导出配置");

        WikiPage rootPage = wikiPageRepository.findById(rootPageId)
                .orElseThrow(() -> new ServiceException("Wiki页面不存在"));

        if(rootPage.getPageType() != PageType.DIRECTORY) {
            throw new ServiceException("只能导出目录类型的页面");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            // 递归导出目录及其子页面
            exportDirectoryRecursive(rootPage, "", zos, exportDTO);

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("导出目录失败", e);
            throw new ServiceException("目录导出失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 递归导出目录
     */
    private void exportDirectoryRecursive(WikiPage page, String pathPrefix,
                                          ZipOutputStream zos, WikiExportDTO exportDTO) throws IOException {
        // 获取子页面
        List<WikiPage> children = wikiPageRepository.findChildPages(page.getProjectId(), page.getId());

        for (WikiPage child : children) {
            // 使用安全的路径分隔符
            String safeTitle = FileUtils.getSafeFilename(child.getTitle());
            String currentPath = pathPrefix + safeTitle;

            if (child.getPageType() == PageType.DIRECTORY) {
                // 如果是目录，递归处理
                exportDirectoryRecursive(child, currentPath + "/", zos, exportDTO);
            } else {
                // 如果是文档，导出内容
                try {
                    byte[] content = exportPage(child.getId(), exportDTO);
                    String fileName = currentPath + getFileExtension(exportDTO.getFormat());

                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    zos.write(content);
                    zos.closeEntry();

                    log.debug("导出页面: {}", fileName);
                }catch (ServiceException e) {
                    log.error("导出页面失败: pageId={}", child.getId(), e);
                }
            }
        }
    }

    /**
     * 导出为Markdown格式（新架构：内容直接在 WikiPage.content）
     */
    private byte[] exportToMarkdown(WikiPage page, WikiExportDTO exportDTO) {
        if (page.getPageType() == PageType.DIRECTORY) {
            throw new ServiceException("目录类型不能导出为单个Markdown，请使用导出目录树功能");
        }

        StringBuilder markdown = new StringBuilder();

        // 标题
        markdown.append("# ").append(page.getTitle()).append("\n\n");

        // 元数据（YAML front matter）
        markdown.append("---\n");
        markdown.append("title: ").append(page.getTitle()).append("\n");
        markdown.append("id: ").append(page.getId()).append("\n");
        markdown.append("创建时间: ").append(formatDateTime(page.getCreatedAt())).append("\n");
        markdown.append("更新时间: ").append(formatDateTime(page.getUpdatedAt())).append("\n");
        markdown.append("版本: ").append(page.getCurrentVersion()).append("\n");
        if (page.getPath() != null) {
            markdown.append("路径: ").append(page.getPath()).append("\n");
        }
        markdown.append("导出时间: ").append(DateUtils.getTime()).append("\n");
        markdown.append("---\n\n");

        // 内容直接从实体获取
        String content = page.getContent();
        if (content != null) {
            markdown.append(content);
        } else {
            markdown.append("*内容为空*\n");
        }

        return markdown.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 导出为PDF格式（TODO：后续可接入 iText 等库）
     */
    private byte[] exportToPdf(WikiPage page, WikiExportDTO exportDTO) {
        throw new ServiceException("PDF导出功能暂未实现，敬请期待");
    }

    /**
     * 导出为Word格式（TODO：后续可接入 Apache POI）
     */
    private byte[] exportToWord(WikiPage page, WikiExportDTO exportDTO) {
        throw new ServiceException("Word导出功能暂未实现，敬请期待");
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 解析Markdown内容，抽取第一个标题为文档标题，其余为正文
     */
    private MarkdownPage parseMarkdown(String content) {
        MarkdownPage page = new MarkdownPage();

        // 提取标题（第一个 # 标题）
        Pattern titlePattern = java.util.regex.Pattern.compile("^#\\s+(.+)$", java.util.regex.Pattern.MULTILINE);
        Matcher titleMatcher = titlePattern.matcher(content);

        if (titleMatcher.find()) {
            page.setTitle(titleMatcher.group(1).trim());

            // 移除标题后的内容作为正文
            int titleEnd = titleMatcher.end();
            String bodyContent = content.substring(titleEnd).trim();

            // 移除 YAML front matter
            bodyContent = removeMetadata(bodyContent);
            page.setContent(bodyContent);
        } else {
            // 如果没有标题，使用默认标题（使用日期时间生成唯一名称）
            String defaultTitle = "未命名文档_" + DateUtils.dateTimeNow();
            page.setTitle(defaultTitle);
            page.setContent(content);
        }

        return page;
    }

    /**
     * 移除Markdown中的元数据部分（YAML front matter）
     */
    private String removeMetadata(String content) {
        Pattern metadataPattern = Pattern.compile("^---\\s*\\n.*?\\n---\\s*\\n",
               Pattern.DOTALL);
       Matcher matcher = metadataPattern.matcher(content);

        if (matcher.find()) {
            return content.substring(matcher.end()).trim();
        }

        return content;
    }

    /**
     * 获取导出格式
     */
    private ExportFormat getExportFormat(String format) {
        if (format == null || format.isEmpty()) {
            return ExportFormat.MARKDOWN;
        }
        try {
            return ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("不支持的导出格式: " + format);
        }
    }

    /**
     * 生成导出文件名
     */
    private String generateFileName(WikiPage page, String format) {
        String extension = getFileExtension(format);
        String safeName = FileUtils.getSafeFilename(page.getTitle());
        return String.format("%s_%s%s", safeName,
                DateUtils.dateTimeNow("yyyyMMddHHmmss"), extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String format) {
        ExportFormat exportFormat = getExportFormat(format);
        return exportFormat.getExtension();
    }

    /**
     * 清理文件名（移除非法字符）
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "untitled";
        }
        // 替换 Windows 和 Unix 文件系统中的非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .trim();
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        return dateTime.format(DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD_HH_MM_SS));
    }

    /**
     * Markdown 页面内部类
     */
    @Setter
    @Getter
    private static class MarkdownPage {
        private String title;
        private String content;
    }
}
