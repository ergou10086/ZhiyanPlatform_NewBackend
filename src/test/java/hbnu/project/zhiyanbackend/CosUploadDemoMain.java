package hbnu.project.zhiyanbackend;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import hbnu.project.zhiyanbackend.oss.config.COSConfig;
import hbnu.project.zhiyanbackend.oss.config.COSProperties;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 独立 COS 上传测试 Demo
 * 直接复用项目中的 COSConfig / COSProperties，从当前环境变量读取配置，
 * 向同一个 bucket 上传一个小的文本对象，用于验证 SignatureDoesNotMatch 问题。
 */
public class CosUploadDemoMain {

    public static void main(String[] args) {
        // 启动一个精简的 Spring 容器，只加载与 COS 相关的配置
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ZhiyanBackendApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        try {
            COSProperties cosProperties = context.getBean(COSProperties.class);
            COSClient cosClient = context.getBean(COSClient.class);

            String bucketName = cosProperties.getBucketName();
            String key = "cos-demo-test/" + LocalDateTime.now().toString().replace(":", "-") + ".txt";
            String content = "COS demo upload test at " + LocalDateTime.now();

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType("text/plain; charset=utf-8");

            PutObjectRequest request = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(bytes), metadata);

            System.out.println("== COS Demo Upload ==");
            System.out.println("Region: " + cosProperties.getRegion());
            System.out.println("Bucket: " + bucketName);
            System.out.println("Key   : " + key);

            try {
                PutObjectResult result = cosClient.putObject(request);
                System.out.println("Upload SUCCESS. ETag=" + result.getETag());
            } catch (CosServiceException e) {
                System.err.println("Upload FAILED (Service): statusCode=" + e.getStatusCode()
                        + ", errorCode=" + e.getErrorCode()
                        + ", errorMessage=" + e.getErrorMessage());
                e.printStackTrace();
            } catch (CosClientException e) {
                System.err.println("Upload FAILED (Client): " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            context.close();
        }
    }
}
