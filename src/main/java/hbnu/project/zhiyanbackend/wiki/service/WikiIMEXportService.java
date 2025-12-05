package hbnu.project.zhiyanbackend.wiki.service;

import hbnu.project.zhiyanbackend.wiki.model.dto.WikiExportDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiImportDTO;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiImportResultDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Wiki 导入导出服务接口
 * 合并导入（Import）与导出（Export）相关能力
 *
 * @author ErgouTree
 */
public interface WikiIMEXportService {

    // ========== 导入相关 ==========

    /**
     * 从单个 Markdown 文件导入 Wiki 页面
     *
     * @param file      Markdown 文件
     * @param importDTO 导入配置
     * @return 导入结果
     */
    WikiImportResultDTO importFromMarkdown(MultipartFile file, WikiImportDTO importDTO);

    /**
     * 批量导入多个 Markdown 文件
     *
     * @param files     文件数组
     * @param importDTO 导入配置
     * @return 汇总导入结果
     */
    WikiImportResultDTO importMultipleMarkdown(MultipartFile[] files, WikiImportDTO importDTO);

    // ========== 导出相关 ==========

    /**
     * 导出单个 Wiki 页面
     *
     * @param pageId    页面ID
     * @param exportDTO 导出配置
     * @return 导出的文件内容（字节数组）
     */
    byte[] exportPage(Long pageId, WikiExportDTO exportDTO);

    /**
     * 批量导出多个 Wiki 页面（打包为 ZIP）
     *
     * @param pageIds   页面ID列表
     * @param exportDTO 导出配置
     * @return ZIP 文件内容
     */
    byte[] exportPages(List<Long> pageIds, WikiExportDTO exportDTO);

    /**
     * 导出整个目录树（打包为 ZIP）
     *
     * @param rootPageId 根页面ID（目录）
     * @param exportDTO  导出配置
     * @return ZIP 文件内容
     */
    byte[] exportDirectory(Long rootPageId, WikiExportDTO exportDTO);
}
