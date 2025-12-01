package hbnu.project.zhiyanbackend.wiki.utils;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.exception.UtilException;
import hbnu.project.zhiyanbackend.wiki.model.entity.ChangeStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 内容差异对比服务
 * 使用 java-diff-utils 实现差异计算、补丁应用与回退、哈希生成及变更统计
 *
 * @author ErgouTree
 * @rewrite ErgouTree,yui
 */
@Slf4j
@Component
public class WikiDiffUtils {

    private static final String EMPTY_CONTENT = "";
    private static final int CONTEXT_LINES = 3;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String DIFF_OLD_LABEL = "original";
    private static final String DIFF_NEW_LABEL = "modified";

    /**
     * 计算两个文本内容的差异
     * 生成Unified Diff格式，类似Git Diff
     *
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return Unified Diff格式的差异字符串，若内容相同则返回空字符串
     */
    public String calculateDiff(String oldContent, String newContent) {
        String safeOldContent = normalizeContent(oldContent);
        String safeNewContent = normalizeContent(newContent);

        // 快速路径：内容相同时直接返回
        if (safeOldContent.equals(safeNewContent)) {
            return EMPTY_CONTENT;
        }

        List<String> oldLines = splitLines(safeOldContent);
        List<String> newLines = splitLines(safeNewContent);

        try{
            Patch<String> patch = DiffUtils.diff(oldLines, newLines);

            // 无差异时返回空字符串
            if (patch.getDeltas().isEmpty()) {
                return EMPTY_CONTENT;
            }

            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                    DIFF_OLD_LABEL,
                    DIFF_NEW_LABEL,
                    oldLines,
                    patch,
                    CONTEXT_LINES
            );

            return String.join("\n", unifiedDiff);
        }catch (Exception e){
            log.error("计算 Diff 失败 - oldContent length: {}, newContent length: {}",
                    safeOldContent.length(), safeNewContent.length(), e);
            throw new UtilException("Diff 计算失败", e);
        }
    }

    /**
     * 应用差异补丁到原始内容
     * 将 Unified Diff 格式的补丁应用到基础内容上，生成新版本内容
     *
     * @param baseContent 基础内容（应用补丁的目标，可为 null）
     * @param diffPatch   Unified Diff 格式的补丁字符串
     * @return 应用补丁后的新内容
     */
    public String applyPatch(String baseContent, String diffPatch) {
        Objects.requireNonNull(diffPatch, "补丁内容不能为 null");

        String safeBaseContent = normalizeContent(baseContent);

        // 空补丁直接返回原内容
        if (diffPatch.trim().isEmpty()) {
            return safeBaseContent;
        }

        try {
            List<String> baseLines = splitLines(safeBaseContent);
            List<String> patchLines = splitLines(diffPatch);

            Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines);
            List<String> resultLines = patch.applyTo(baseLines);

            return String.join("\n", resultLines);
        }catch (UtilException e){
            log.error("应用补丁失败 - baseContent length: {}, patch length: {}",
                    safeBaseContent.length(), diffPatch.length(), e);
            throw new UtilException("补丁应用失败，可能是基础内容已发生变化", e);
        }catch (Exception e) {
            log.error("解析补丁失败 - patch: {}", diffPatch, e);
            throw new ServiceException("补丁格式错误", e);
        }
    }

    /**
     * 逆向应用补丁（回退内容）
     * <p>
     * 从新内容回退到旧内容，用于版本回滚操作
     * 对"旧→新"的补丁反向解析，生成"新→旧"的回退效果
     *
     * @param newContent 当前内容（需要回退的版本，可为 null）
     * @param diffPatch  "旧→新" 的差异补丁
     * @return 回退后的旧内容
     */
    public String reversePatch(String newContent, String diffPatch) {
        Objects.requireNonNull(diffPatch, "补丁内容不能为 null");

        String safeNewContent = normalizeContent(newContent);

        if (diffPatch.trim().isEmpty()) {
            return safeNewContent;
        }

        try {
            List<String> newLines = splitLines(safeNewContent);
            List<String> patchLines = splitLines(diffPatch);

            Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines);

            // 使用 restore 方法从新内容恢复旧内容
            List<String> oldLines = patch.restore(newLines);

            return String.join("\n", oldLines);
        } catch (UtilException e) {
            log.error("逆向应用补丁失败 - newContent length: {}, patch length: {}",
                    safeNewContent.length(), diffPatch.length(), e);
            throw new UtilException("补丁回退失败，可能是内容版本不匹配", e);
        } catch (Exception e) {
            log.error("解析补丁失败（逆向操作）- patch: {}", diffPatch, e);
            throw new UtilException("补丁格式错误，无法回退", e);
        }
    }

    /**
     * 计算内容的SHA-256哈希值
     * 用于快速判断内容是否发生变化，避免不必要的 Diff 计算
     *
     * @param content 待计算哈希的内容
     * @return 小写的SHA-256十六进制哈希字符串
     */
    public String calculateHash(String content) {
        String safeContent = normalizeContent(content);

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(safeContent.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法，理论上不会发生
            log.error("SHA-256 算法不可用，JDK 环境异常", e);
            throw new UtilException("哈希计算失败", e);
        }
    }

    /**
     * 验证内容哈希是否匹配
     *
     * @param content      待验证的内容
     * @param expectedHash 期望的哈希值
     * @return true 表示哈希匹配
     */
    public boolean verifyHash(String content, String expectedHash) {
        if (expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }
        String actualHash = calculateHash(content);
        return actualHash.equalsIgnoreCase(expectedHash);
    }

    /**
     * 计算内容变更统计
     * 提供行级和字符级的变更量化指标
     *
     * @param oldContent 旧内容（可为 null）
     * @param newContent 新内容（可为 null）
     * @return 变更统计对象
     */
    public ChangeStats calculateStats(String oldContent, String newContent) {
        String safeOldContent = normalizeContent(oldContent);
        String safeNewContent = normalizeContent(newContent);

        // 快速路径：内容相同时返回零变更
        if (safeOldContent.equals(safeNewContent)) {
            return ChangeStats.zero();
        }

        List<String> oldLines = splitLines(safeOldContent);
        List<String> newLines = splitLines(safeNewContent);

        try {
            Patch<String> patch = DiffUtils.diff(oldLines, newLines);

            int addedLines = 0;
            int deletedLines = 0;

            for(var delta : patch.getDeltas()) {
                switch (delta.getType()) {
                    case INSERT:
                        addedLines += delta.getTarget().size();
                        break;
                    case DELETE:
                        deletedLines += delta.getSource().size();
                        break;
                    case CHANGE:
                        addedLines += delta.getTarget().size();
                        deletedLines += delta.getSource().size();
                        break;
                }
            }

            int changedChars = Math.abs(safeNewContent.length() - safeOldContent.length());

            return ChangeStats.builder()
                    .addedLines(addedLines)
                    .deletedLines(deletedLines)
                    .changedChars(changedChars)
                    .build();
        }catch (UtilException e) {
            log.error("计算变更统计失败", e);
            return ChangeStats.zero();
        }
    }

    /**
     * 规范化内容（处理 null）
     */
    private String normalizeContent(String content) {
        return content == null ? EMPTY_CONTENT : content;
    }

    /**
     * 按行分割内容
     * 使用 -1 作为 limit 参数，保留尾部空行
     */
    private List<String> splitLines(String content) {
        if (content.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(content.split("\n", -1));
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}