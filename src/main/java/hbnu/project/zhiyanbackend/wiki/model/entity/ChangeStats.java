package hbnu.project.zhiyanbackend.wiki.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容变更统计实体
 * 用于量化内容版本间的差异
 *
 * @author ErgouTree
 * @rewrite yui
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStats {

    /**
     * 新增的行数
     */
    private int addedLines;

    /**
     * 删除的行数
     */
    private int deletedLines;

    /**
     * 变更的字符数（新旧内容长度差的绝对值）
     */
    private int changedChars;

    /**
     * 获取总变更行数
     *
     * @return 新增行数 + 删除行数
     */
    public int getTotalChangedLines() {
        return addedLines + deletedLines;
    }

    /**
     * 获取净变更行数（可能为负值）
     *
     * @return 新增行数 - 删除行数
     */
    public int getNetLineChange() {
        return addedLines - deletedLines;
    }

    /**
     * 判断是否有变更
     *
     * @return true 表示有任何变更
     */
    public boolean hasChanges() {
        return addedLines > 0 || deletedLines > 0 || changedChars > 0;
    }

    /**
     * 创建零变更统计对象
     *
     * @return 所有指标为 0 的统计对象
     */
    public static ChangeStats zero() {
        return ChangeStats.builder()
                .addedLines(0)
                .deletedLines(0)
                .changedChars(0)
                .build();
    }

    /**
     * 获取变更摘要描述
     *
     * @return 易读的变更描述，如 "+10 -5 ~20 chars"
     */
    public String getSummary() {
        if (!hasChanges()) {
            return "无变更";
        }
        return String.format("+%d -%d (~%d chars)", addedLines, deletedLines, changedChars);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}