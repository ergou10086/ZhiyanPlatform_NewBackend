package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2FA设置响应DTO
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupDTO {
    /**
     * 密钥（Base32编码）
     */
    private String secretKey;

    /**
     * 二维码Base64图片
     */
    private String qrCodeBase64;

    /**
     * 二维码内容（备用，用于手动输入）
     */
    private String qrCodeText;
}