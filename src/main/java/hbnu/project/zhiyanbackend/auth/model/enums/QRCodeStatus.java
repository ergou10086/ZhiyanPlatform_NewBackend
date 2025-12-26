package hbnu.project.zhiyanbackend.auth.model.enums;

import lombok.Getter;

/**
 * 二维码登陆状态枚举
 *
 * @author ErgouTree
 */
@Getter
public enum QRCodeStatus {

    /**
     * 待扫描
     */
    PENDING("待扫描"),

    /**
     * 已扫描 - 移动端已扫描,等待用户确认
     */
    SCANNED("已扫描"),

    /**
     * 已确认 - 用户已确认,PC端可以登录
     */
    CONFIRMED("已确认"),

    /**
     * 已过期 - 二维码已过期(默认5分钟)
     */
    EXPIRED("已过期"),

    /**
     * 已取消 - 用户取消登录
     */
    CANCELLED("已取消");

    private final String description;

    QRCodeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
