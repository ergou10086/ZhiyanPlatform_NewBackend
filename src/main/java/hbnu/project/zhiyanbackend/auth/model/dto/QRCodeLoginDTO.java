package hbnu.project.zhiyanbackend.auth.model.dto;

import hbnu.project.zhiyanbackend.auth.model.enums.QRCodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 扫码登录数据传输对象
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeLoginDTO {

    /**
     * 二维码ID
     */
    private String qrCodeId;

    /**
     * 二维码内容,用于生成二维码图片
     * 格式: zhiyan://qrlogin?code=xxx
     */
    private String qrCodeContent;

    /**
     * 二维码Base64图
     */
    private String qrCodeBase64;

    /**
     * 二维码状态
     */
    private QRCodeStatus status;

    /**
     * 扫描用户信息
     */
    private ScanUserInfo scanUser;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 剩余有效时间(秒)
     */
    private Long remainingSeconds;


    /**
     * 扫描用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanUserInfo {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名
         */
        private String name;

        /**
         * 用户邮箱
         * 脱敏
         */
        private String email;

        /**
         * 头像 URL（COS）
         */
        private String avatarUrl;
    }
}
