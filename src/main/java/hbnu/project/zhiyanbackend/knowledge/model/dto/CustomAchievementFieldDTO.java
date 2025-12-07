package hbnu.project.zhiyanbackend.knowledge.model.dto;

import hbnu.project.zhiyanbackend.security.xss.Xss;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 自定义字段DTO
 * 用于定义成果的自定义字段
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAchievementFieldDTO {

    /**
     * 字段键（英文，用于存储）
     */
    @NotBlank(message = "字段键不能为空")
    @Xss(message = "字段键包含非法字符")
    private String fieldKey;

    /**
     * 字段名称（中文，用于显示）
     */
    @NotBlank(message = "字段名称不能为空")
    @Xss(message = "字段名称包含非法字符")
    private String fieldLabel;

    /**
     * 字段类型：text, number, date, textarea, select, multiselect
     */
    @NotBlank(message = "字段类型不能为空")
    @Xss(message = "字段类型包含非法字符")
    private String fieldType;

    /**
     * 是否必填
     */
    @Builder.Default
    private Boolean required = false;

    /**
     * 默认值
     */
    @Xss(message = "默认值包含非法字符")
    private String defaultValue;

    /**
     * 占位符
     */
    @Xss(message = "占位符包含非法字符")
    private String placeholder;

    /**
     * 选项（当fieldType为select或multiselect时使用）
     */
    private String[] options;

    /**
     * 验证规则（正则表达式）
     */
    private String validationRule;

    /**
     * 提示信息
     */
    @Xss(message = "提示信息包含非法字符")
    private String helpText;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
