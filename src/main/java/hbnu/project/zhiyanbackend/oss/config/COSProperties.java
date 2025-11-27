package hbnu.project.zhiyanbackend.oss.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 腾讯云 COS 基础配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "tencent.cos")
public class COSProperties {

    /**
     * 访问凭证 SecretId
     */
    @NotBlank(message = "COS SecretId 不能为空")
    private String secretId;

    /**
     * 访问凭证 SecretKey
     */
    @NotBlank(message = "COS SecretKey 不能为空")
    private String secretKey;

    /**
     * 区域，如 ap-beijing
     */
    @NotBlank(message = "COS 区域不能为空")
    private String region;

    /**
     * 默认存储桶名称
     * 单桶策略
     */
    @NotBlank(message = "COS 桶名称不能为空")
    private String bucketName;

    /**
     * COS公网访问域名
     * TODO:先用创建桶给的默认的，之后备案过了换成自己的
     */
    @NotBlank(message = "COS 公网访问域名不能为空")
    private String publicDomain;

    /**
     * 未指定目录时的默认目录
     */
    private String defaultDirectory = "uploads";

    /**
     * TransferManager 配置
     */
    @NotNull
    private TransferConfig transfer = new TransferConfig();

    /**
     * TransferManager 传输配置
     */
    @Data
    public static class TransferConfig {
        /**
         * 线程池大小（用于并发上传分块）
         */
        private int threadPoolSize = 16;

        /**
         * 分块上传阈值（字节）
         * 大于此值的文件将使用分块上传，默认 50MB
         */
        private long multipartThreshold = 50 * 1024 * 1024L;

        /**
         * 分块大小（字节）
         * 每个分块的大小，默认 5MB
         */
        private long partSize = 5 * 1024 * 1024L;

        /**
         * 是否启用断点续传
         */
        private boolean enableResumable = true;
    }

    /**
     * 业务类型配置
     */
    @NotNull
    private BusinessTypeConfig businessType = new BusinessTypeConfig();

    /**
     * 业务类型目录配置
     */
    @Data
    public static class BusinessTypeConfig {
        // 临时文件桶
        private String temporary = "temporary_file";
        // 成果附件桶
        private String achievement = "achievement_file";
        // wiki附件资源桶
        private String wiki = "wiki_file";
        // 任务提交相关文件桶
        private String task = "task_file";
    }
}

