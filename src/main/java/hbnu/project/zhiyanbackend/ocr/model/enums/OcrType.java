package hbnu.project.zhiyanbackend.ocr.model.enums;

import lombok.Getter;

/**
 * OCR类型枚举
 *
 * @author ErgouTree
 */
@Getter
public enum OcrType {
    STANDARD("standard", "通用文字识别-标准版", 10),
    STANDARD_POS("standard-pos", "通用文字识别-标准版含位置", 5),
    HIGH_ACCURACY("high-accuracy", "通用文字识别-高精度版", 5),
    HIGH_ACCURACY_POS("high-accuracy-pos", "通用文字识别-高精度含位置版", 5);

    private final String code;
    private final String description;
    private final Integer dailyLimit;

    OcrType(String code, String description, Integer dailyLimit) {
        this.code = code;
        this.description = description;
        this.dailyLimit = dailyLimit;
    }

    /**
     * 根据代码获取OCR类型
     */
    public static OcrType fromCode(String code) {
        for (OcrType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的OCR类型: " + code);
    }
}
