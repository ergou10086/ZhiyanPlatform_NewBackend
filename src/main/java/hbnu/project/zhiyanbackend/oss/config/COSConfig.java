package hbnu.project.zhiyanbackend.oss.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 腾讯云 COS 客户端配置
 *
 * @author ErgouTree
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(COSProperties.class)
public class COSConfig {

    /**
     * 创建 COSClient Bean
     *
     * @param cosProperties cos配置
     * @return 实例
     */
    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient(COSProperties cosProperties) {
        // 创建身份凭证
        COSCredentials credentials = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());
        // cosclient 的一些配置
        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        log.info("COSClient 初始化成功: region={}, bucket={}", cosProperties.getRegion(), cosProperties.getBucketName());
        return new COSClient(credentials, clientConfig);
    }

    /**
     * 创建线程池 Bean
     * 用于 TransferManager 并发上传分块
     *
     * @param cosProperties COS 配置属性
     * @return ExecutorService 实例
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService cosThreadPool(COSProperties cosProperties) {
        int poolSize = cosProperties.getTransfer().getThreadPoolSize();
        log.info("COS 线程池初始化: size={}", poolSize);
        return Executors.newFixedThreadPool(poolSize);
    }

    /**
     * 创建 TransferManager Bean
     * TransferManager 是线程安全的，应用全局复用
     *
     * @param cosClient COS 客户端
     * @param cosThreadPool 线程池
     * @param cosProperties COS 配置属性
     * @return TransferManager 实例
     */
    @Bean(destroyMethod = "")
    public TransferManager transferManager(COSClient cosClient,
                                           ExecutorService cosThreadPool,
                                           COSProperties cosProperties) {
        // 创建 TransferManager 配置
        TransferManagerConfiguration config = new TransferManagerConfiguration();
        config.setMultipartUploadThreshold(cosProperties.getTransfer().getMultipartThreshold());
        config.setMinimumUploadPartSize(cosProperties.getTransfer().getPartSize());

        // 创建 TransferManager 实例
        TransferManager transferManager = new TransferManager(cosClient, cosThreadPool);
        transferManager.setConfiguration(config);

        log.info("TransferManager 初始化成功: multipartThreshold={} bytes, partSize={} bytes",
                config.getMultipartUploadThreshold(),
                config.getMinimumUploadPartSize());

        return transferManager;
    }
}


