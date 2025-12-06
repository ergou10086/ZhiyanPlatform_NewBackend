package hbnu.project.zhiyanbackend.security.encrypt.config.properties;

import hbnu.project.zhiyanbackend.security.encrypt.enumd.AlgorithmType;
import hbnu.project.zhiyanbackend.security.encrypt.enumd.EncodeType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 加解密配置属性类
 * 统一管理加密相关的配置
 *
 * @author ErgouTree
 * @version 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "zhiyan.encrypt")
public class EncryptorProperties {

    /**
     * 是否启用加密功能
     */
    private Boolean enabled = false;

    /**
     * 默认加密算法
     */
    private AlgorithmType algorithm = AlgorithmType.AES;

    /**
     * 默认编码方式
     */
    private EncodeType encode = EncodeType.BASE64;

    /**
     * AES 密钥（16/24/32位）
     */
    private String aesKey = "zhiyan1234567890";

    /**
     * SM4 密钥（16位）
     */
    private String sm4Key = "zhiyan1234567890";

    /**
     * RSA 公钥
     */
    private String rsaPublicKey;

    /**
     * RSA 私钥
     */
    private String rsaPrivateKey;

    /**
     * SM2 公钥
     */
    private String sm2PublicKey;

    /**
     * SM2 私钥
     */
    private String sm2PrivateKey;

    /**
     * API 加密配置
     */
    private ApiConfig api = new ApiConfig();

    /**
     * 获取密钥（通用方法，用于对称加密算法）
     * 根据当前算法类型返回对应的密钥
     *
     * @return 密钥
     */
    public String getPassword() {
        return switch (this.algorithm) {
            case AES -> this.aesKey;
            case SM4 -> this.sm4Key;
            // 默认返回AES密钥
            default -> this.aesKey;
        };
    }

    /**
     * 获取公钥（通用方法，用于非对称加密算法）
     * 根据当前算法类型返回对应的公钥
     *
     * @return 公钥
     */
    public String getPublicKey() {
        return switch (this.algorithm) {
            case RSA -> this.rsaPublicKey;
            case SM2 -> this.sm2PublicKey;
            // 默认返回RSA公钥
            default -> this.rsaPublicKey;
        };
    }

    /**
     * 获取私钥（通用方法，用于非对称加密算法）
     * 根据当前算法类型返回对应的私钥
     *
     * @return 私钥
     */
    public String getPrivateKey() {
        return switch (this.algorithm) {
            case RSA -> this.rsaPrivateKey;
            case SM2 -> this.sm2PrivateKey;
            // 默认返回RSA私钥
            default -> this.rsaPrivateKey;
        };
    }

    /**
     * API 加密配置
     */
    @Data
    public static class ApiConfig {
        /**
         * 是否启用 API 加密
         */
        private Boolean enabled = false;

        /**
         * 需要加密的路径
         */
        private String[] encryptPaths = new String[]{};

        /**
         * 排除的路径
         */
        private String[] excludePaths = new String[]{"/actuator/**", "/error"};

        /**
         * 加密标识请求头名称
         */
        private String encryptHeader = "X-Encrypt";

        /**
         * 算法标识请求头名称
         */
        private String algorithmHeader = "X-Algorithm";
    }
}
