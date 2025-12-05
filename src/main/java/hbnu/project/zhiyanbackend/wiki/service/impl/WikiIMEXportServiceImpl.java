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

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.commonmark.node.ListItem; // 关键：导入正确的 ListItem
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        markdown.append(Objects.requireNonNullElse(content, "*内容为空*\n"));

        return markdown.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 导出为PDF格式（使用 iText 5.x）
     */
    private byte[] exportToPdf(WikiPage page, WikiExportDTO exportDTO) {
        if (page.getPageType() == PageType.DIRECTORY) {
            throw new ServiceException("目录类型不能导出为单个PDF，请使用导出目录树功能");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 创建 PDF 文档
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // 设置中文字体（使用 itext-asian 库支持的中文字体）
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 24, Font.BOLD);
            Font heading1Font = new Font(baseFont, 20, Font.BOLD);
            Font heading2Font = new Font(baseFont, 18, Font.BOLD);
            Font heading3Font = new Font(baseFont, 16, Font.BOLD);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font codeFont = new Font(baseFont, 10, Font.NORMAL);
            codeFont.setColor(0, 102, 204); // 代码块使用蓝色

            // 添加标题
            Paragraph title = new Paragraph(page.getTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 添加元数据
            Font metaFont = new Font(baseFont, 10, Font.ITALIC);
            metaFont.setColor(128, 128, 128);
            Paragraph metadata = new Paragraph();
            metadata.add(new Chunk("ID: " + page.getId() + "  ", metaFont));
            metadata.add(new Chunk("创建时间: " + formatDateTime(page.getCreatedAt()) + "  ", metaFont));
            metadata.add(new Chunk("更新时间: " + formatDateTime(page.getUpdatedAt()) + "  ", metaFont));
            metadata.add(new Chunk("版本: " + page.getCurrentVersion(), metaFont));
            metadata.setSpacingAfter(15);
            document.add(metadata);

            // 添加分隔线
            document.add(new Paragraph("────────────────────────────────────────", normalFont));
            document.add(new Paragraph(" "));

            // 解析 Markdown 内容
            String content = page.getContent();
            if (content == null || content.trim().isEmpty()) {
                Paragraph emptyContent = new Paragraph("内容为空", normalFont);
                document.add(emptyContent);
            } else {
                // 使用 CommonMark 解析 Markdown
                Parser parser = Parser.builder().build();
                Node documentNode = parser.parse(content);
                
                // 遍历节点并转换为 PDF 元素
                convertMarkdownToPdf(documentNode, document, baseFont, heading1Font, heading2Font, 
                        heading3Font, normalFont, codeFont);
            }

            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("PDF导出失败: pageId={}, title={}", page.getId(), page.getTitle(), e);
            throw new ServiceException("PDF导出失败: " + e.getMessage());
        }
    }

    /**
     * 将 Markdown 节点转换为 PDF 元素
     */
    private void convertMarkdownToPdf(Node node, Document document, BaseFont baseFont,
                                      Font heading1Font, Font heading2Font, Font heading3Font,
                                      Font normalFont, Font codeFont) throws DocumentException {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof Heading heading) {
                int level = heading.getLevel();
                Font font = switch (level) {
                    case 1 -> heading1Font;
                    case 2 -> heading2Font;
                    case 3 -> heading3Font;
                    default -> normalFont;
                };
                Paragraph para = buildParagraphFromNode(heading, font, baseFont, normalFont, codeFont);
                para.setSpacingBefore(level == 1 ? 15 : level == 2 ? 12 : 10);
                para.setSpacingAfter(8);
                document.add(para);
            } else if (child instanceof org.commonmark.node.Paragraph) {
                Paragraph para = buildParagraphFromNode(child, normalFont, baseFont, normalFont, codeFont);
                para.setSpacingAfter(8);
                document.add(para);
            } else if (child instanceof BulletList || child instanceof OrderedList) {
                convertListToPdf(child, document, baseFont, normalFont, codeFont);
            } else if (child instanceof FencedCodeBlock) {
                String code = ((FencedCodeBlock) child).getLiteral();
                addCodeBlock(document, code, codeFont);
            } else if (child instanceof IndentedCodeBlock) {
                String code = ((IndentedCodeBlock) child).getLiteral();
                addCodeBlock(document, code, codeFont);
            } else if (child instanceof BlockQuote) {
                Paragraph quote = buildParagraphFromNode(child, normalFont, baseFont, normalFont, codeFont);
                quote.setIndentationLeft(20);
                quote.setSpacingAfter(8);
                document.add(quote);
            } else if (child instanceof ThematicBreak) {
                document.add(new Paragraph("────────────────────────────────────────", normalFont));
                document.add(new Paragraph(" "));
            } else {
                // 递归处理其他节点
                convertMarkdownToPdf(child, document, baseFont, heading1Font, 
                        heading2Font, heading3Font, normalFont, codeFont);
            }
            child = child.getNext();
        }
    }

    /**
     * 从节点构建 Paragraph，支持嵌套格式（粗体、斜体、代码等）
     */
    private Paragraph buildParagraphFromNode(Node node, Font defaultFont, BaseFont baseFont,
                                               Font normalFont, Font codeFont) {
        Paragraph para = new Paragraph();
        addChunksFromNode(node, para, defaultFont, baseFont, normalFont, codeFont);
        return para;
    }

    /**
     * 递归添加 Chunk，支持嵌套格式
     */
    private void addChunksFromNode(Node node, Paragraph para, Font currentFont, BaseFont baseFont,
                                   Font normalFont, Font codeFont) {
        Node child = node.getFirstChild();
        while (child != null) {
            switch (child) {
                case Text text1 -> {
                    String text = text1.getLiteral();
                    if (!text.isEmpty()) {
                        para.add(new Chunk(text, currentFont));
                    }
                }
                case StrongEmphasis strongEmphasis -> {
                    Font boldFont = new Font(baseFont, currentFont.getSize(), Font.BOLD);
                    addChunksFromNode(child, para, boldFont, baseFont, normalFont, codeFont);
                }
                case Emphasis emphasis -> {
                    Font italicFont = new Font(baseFont, currentFont.getSize(), Font.ITALIC);
                    addChunksFromNode(child, para, italicFont, baseFont, normalFont, codeFont);
                }
                case Code code1 -> {
                    String code = code1.getLiteral();
                    para.add(new Chunk(code, codeFont));
                }
                default ->
                    // 递归处理其他节点
                        addChunksFromNode(child, para, currentFont, baseFont, normalFont, codeFont);
            }
            child = child.getNext();
        }
    }

    /**
     * 添加代码块
     */
    private void addCodeBlock(Document document, String code, Font codeFont) throws DocumentException {
        // 将代码按行分割，每行作为一个段落
        String[] lines = code.split("\n");
        for (String line : lines) {
            Paragraph codePara = new Paragraph(line.isEmpty() ? " " : line, codeFont);
            codePara.setIndentationLeft(20);
            codePara.setSpacingAfter(2);
            document.add(codePara);
        }
        document.add(new Paragraph(" ")); // 代码块后添加空行
    }

    /**
     * 转换列表为 PDF
     */
    private void convertListToPdf(Node listNode, Document document, BaseFont baseFont,
                                  Font normalFont, Font codeFont) throws DocumentException {
        boolean isOrdered = listNode instanceof OrderedList;
        int itemNumber = 1;
        
        Node item = listNode.getFirstChild();
        while (item != null) {
            if (item instanceof ListItem) {
                String prefix = isOrdered ? (itemNumber + ". ") : "• ";
                Paragraph para = new Paragraph();
                para.add(new Chunk(prefix, normalFont));
                addChunksFromNode(item, para, normalFont, baseFont, normalFont, codeFont);
                para.setIndentationLeft(20);
                para.setSpacingAfter(5);
                document.add(para);
                itemNumber++;
            }
            item = item.getNext();
        }
        document.add(new Paragraph(" ")); // 列表后添加空行
    }

    /**
     * 获取节点的文本内容
     */
    private String getTextContent(Node node) {
        StringBuilder text = new StringBuilder();
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof Text) {
                text.append(((Text) child).getLiteral());
            } else if (child instanceof Code) {
                text.append(((Code) child).getLiteral());
            } else {
                text.append(getTextContent(child));
            }
            child = child.getNext();
        }
        return text.toString().trim();
    }

    /**
     * 导出为Word格式
     * 使用hutool-poi
     */
    private byte[] exportToWord(WikiPage page, WikiExportDTO exportDTO) {
        if (page.getPageType() == PageType.DIRECTORY) {
            throw new ServiceException("目录类型不能导出为单个Word，请使用导出目录树功能");
        }

        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            // 创建 Word 文档
            XWPFDocument document = new XWPFDocument();

            // 添加标题
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(page.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(24);
            titleRun.setFontFamily("宋体");
            titlePara.setSpacingAfter(400); // 20pt

            // 添加元数据
            XWPFParagraph metaPara = document.createParagraph();
            metaPara.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun metaRun = metaPara.createRun();
            metaRun.setText("ID: " + page.getId() + "  ");
            metaRun.setItalic(true);
            metaRun.setFontSize(10);
            metaRun.setColor("808080");

            XWPFRun metaRun2 = metaPara.createRun();
            metaRun2.setText("创建时间: " + formatDateTime(page.getCreatedAt()) + "  ");
            metaRun2.setItalic(true);
            metaRun2.setFontSize(10);
            metaRun2.setColor("808080");

            XWPFRun metaRun3 = metaPara.createRun();
            metaRun3.setText("更新时间: " + formatDateTime(page.getUpdatedAt()) + "  ");
            metaRun3.setItalic(true);
            metaRun3.setFontSize(10);
            metaRun3.setColor("808080");

            XWPFRun metaRun4 = metaPara.createRun();
            metaRun4.setText("版本: " + page.getCurrentVersion());
            metaRun4.setItalic(true);
            metaRun4.setFontSize(10);
            metaRun4.setColor("808080");
            metaPara.setSpacingAfter(300);

            // 添加分隔线
            XWPFParagraph linePara = document.createParagraph();
            XWPFRun lineRun = linePara.createRun();
            lineRun.setText("────────────────────────────────────────");
            linePara.setSpacingAfter(200);
            // 接空行
            document.createParagraph();

            // 解析 Markdown 内容
            String content = page.getContent();
            if(content == null || content.trim().isEmpty()){
                XWPFParagraph emptyPara = document.createParagraph();
                XWPFRun emptyRun = emptyPara.createRun();
                emptyRun.setText("内容为空");
                emptyRun.setFontFamily("宋体");
            }else{
                // 使用 CommonMark 解析 Markdown
                Parser parser = Parser.builder().build();
                Node documentNode = parser.parse(content);

                // 遍历节点并转换为 Word 元素
                convertMarkdownToWord(documentNode, document);
            }

            // 写入到字节数组
            document.write(bos);
            document.close();
            return bos.toByteArray();
        }catch (Exception e) {
            log.error("Word导出失败: pageId={}, title={}", page.getId(), page.getTitle(), e);
            throw new ServiceException("Word导出失败: " + e.getMessage());
        }
    }

    /**
     * 将 Markdown 节点转换为 Word 元素
     */
    private void convertMarkdownToWord(Node node, XWPFDocument document) {
        Node child = node.getFirstChild();
        while (child != null) {
            if(child instanceof Heading heading) {
                int level = heading.getLevel();
                XWPFParagraph para = document.createParagraph();

                // 设置标题样式
                int fontSize = switch (level) {
                    case 1 -> 20;
                    case 2 -> 18;
                    case 3 -> 16;
                    default -> 14;
                };

                para.setSpacingBefore(level == 1 ? 300 : level == 2 ? 240 : 200);
                para.setSpacingAfter(160);

                XWPFRun paraRun = para.createRun();
                paraRun.setBold(true);
                paraRun.setFontSize(fontSize);
                paraRun.setFontFamily("宋体");

                // 添加标题文本（支持嵌套格式）
                addRunsFromNode(heading, para, fontSize, true);
                para.removeRun(0); // 移除默认的空 run
            }else if (child instanceof org.commonmark.node.Paragraph) {
                XWPFParagraph para = document.createParagraph();
                para.setSpacingAfter(160);
                addRunsFromNode(child, para, 12, false);
            } else if (child instanceof BulletList || child instanceof OrderedList) {
                convertListToWord(child, document);
            } else if (child instanceof FencedCodeBlock) {
                String code = ((FencedCodeBlock) child).getLiteral();
                addCodeBlock(document, code);
            } else if (child instanceof IndentedCodeBlock) {
                String code = ((IndentedCodeBlock) child).getLiteral();
                addCodeBlock(document, code);
            } else if (child instanceof BlockQuote) {
                XWPFParagraph quote = document.createParagraph();
                quote.setIndentationLeft(400); // 20pt
                quote.setSpacingAfter(160);
                XWPFRun quoteRun = quote.createRun();
                quoteRun.setItalic(true);
                quoteRun.setFontFamily("宋体");
                addRunsFromNode(child, quote, 12, false);
            } else if (child instanceof ThematicBreak) {
                XWPFParagraph linePara = document.createParagraph();
                XWPFRun lineRun = linePara.createRun();
                lineRun.setText("────────────────────────────────────────");
                linePara.setSpacingAfter(200);
                document.createParagraph(); // 空行
            } else {
                // 递归处理其他节点
                convertMarkdownToWord(child, document);
            }
            child = child.getNext();
        }
    }

    /**
     * 从节点添加 Run(支持嵌套格式)
     */
    private void addRunsFromNode(Node node, XWPFParagraph para, int fontSize, boolean isBold) {
        Node child = node.getFirstChild();
        while (child != null) {
            switch (child) {
                case Text text1 -> {
                    String text = text1.getLiteral();
                    if (!text.isEmpty()) {
                        XWPFRun run = para.createRun();
                        run.setText(text);
                        run.setFontSize(fontSize);
                        run.setFontFamily("宋体");
                        if (isBold) {
                            run.setBold(true);
                        }
                    }
                }
                case StrongEmphasis strongEmphasis -> addRunsFromNode(child, para, fontSize, true);
                case Emphasis emphasis -> {
                    XWPFRun run = para.createRun();
                    String text = getTextContent(child);
                    run.setText(text);
                    run.setItalic(true);
                    run.setFontSize(fontSize);
                    run.setFontFamily("宋体");
                }
                case Code code1 -> {
                    String code = code1.getLiteral();
                    XWPFRun run = para.createRun();
                    run.setText(code);
                    run.setFontSize(fontSize - 1);
                    run.setFontFamily("Courier New");
                    run.setColor("0066CC");
                }
                default ->
                    // 递归处理其他节点
                        addRunsFromNode(child, para, fontSize, isBold);
            }
            child = child.getNext();
        }
    }

    /**
     * 添加代码块
     */
    private void addCodeBlock(XWPFDocument document, String code) {
        // 将代码按行分割
        String[] lines = code.split("\n");
        for (String line : lines) {
            XWPFParagraph codePara = document.createParagraph();
            codePara.setIndentationLeft(400); // 20pt
            codePara.setSpacingAfter(40);
            codePara.setSpacingBefore(40);

            // 设置代码块背景色（灰色）
            codePara.setBorderTop(Borders.SINGLE);
            codePara.setBorderBottom(Borders.SINGLE);
            codePara.setBorderLeft(Borders.SINGLE);
            codePara.setBorderRight(Borders.SINGLE);

            XWPFRun codeRun = codePara.createRun();
            codeRun.setText(line.isEmpty() ? " " : line);
            codeRun.setFontFamily("Courier New");
            codeRun.setFontSize(10);
            codeRun.setColor("0066CC");
        }
        document.createParagraph(); // 代码块后添加空行
    }

    /**
     * 转换列表为 Word
     */
    private void convertListToWord(Node listNode, XWPFDocument document) {
        boolean isOrdered = listNode instanceof OrderedList;
        int itemNumber = 1;

        Node item = listNode.getFirstChild();
        while (item != null) {
            if (item instanceof ListItem) {
                XWPFParagraph para = document.createParagraph();
                para.setIndentationLeft(400); // 20pt
                para.setSpacingAfter(100);

                XWPFRun prefixRun = para.createRun();
                String prefix = isOrdered ? (itemNumber + ". ") : "• ";
                prefixRun.setText(prefix);
                prefixRun.setFontFamily("宋体");
                prefixRun.setFontSize(12);

                // 添加列表项内容（支持嵌套格式）
                addRunsFromNode(item, para, 12, false);

                itemNumber++;
            }
            item = item.getNext();
        }
        document.createParagraph(); // 列表后添加空行
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
