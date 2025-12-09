package hbnu.project.zhiyanbackend.oss.service;

import com.qcloud.cos.model.*;
import hbnu.project.zhiyanbackend.oss.dto.UploadFileResponseDTO;
import com.qcloud.cos.http.HttpMethodName;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * 腾讯云 COS 对象存储服务接口
 *
 * @author ErgouTree
 */
public interface COSService {

    /**
     * 上传文件到默认桶，使用上传流
     * @param file 上传的文件
     * @param businessType 业务类型目录
     * @param filename 自定义文件名
     * @return 上传结果
     */
    UploadFileResponseDTO uploadFile(MultipartFile file, String businessType, String filename);

    /**
     * 上传本地文件到COS
     * @param localFile 本地文件
     * @param businessType 业务类型目录
     * @param filename 自定义文件名
     * @return 上传结果
     */
    UploadFileResponseDTO uploadLocalFile(File localFile, String businessType, String filename);

    /**
     * 上传 MultipartFile 文件，使用对象流（高级接口）
     * @param file 上传的文件
     * @param businessType 业务类型目录
     * @param filename 自定义文件名
     * @return 上传结果
     */
    UploadFileResponseDTO uploadFileSenior(MultipartFile file, String businessType, String filename);

    /**
     * 上传本地文件（高级接口）
     * @param localFile 本地文件
     * @param businessType 业务类型目录
     * @param filename 自定义文件名
     * @return 上传结果
     */
    UploadFileResponseDTO uploadLocalFileSenior(File localFile, String businessType, String filename);

    /**
     * 批量上传文件
     * @param files 文件数组
     * @param businessType 业务类型目录
     * @return 上传结果列表
     */
    List<UploadFileResponseDTO> uploadBatch(MultipartFile[] files, String businessType);

    /**
     * 列出目录下的对象和子目录
     * @param listObjectsRequest 列出对象请求
     * @return 对象列表
     */
    ObjectListing listObjects(ListObjectsRequest listObjectsRequest);

    /**
     * 查询指定对象的元数据信息
     * @param bucketName 存储桶名称
     * @param key 对象键
     * @return 对象元数据
     */
    ObjectMetadata getObjectMetadata(String bucketName, String key);

    /**
     * 获取对象访问 URL
     * @param bucketName 存储桶名称
     * @param key 对象键
     * @return 访问URL
     */
    URL getObjectUrl(String bucketName, String key);

    /**
     * 列举指定前缀的所有对象(完整版)
     *
     * @param bucketName 存储桶名称
     * @param prefix 前缀
     * @param delimiter 分隔符
     * @return 对象列表
     */
    List<COSObjectSummary> listObjectsByPrefix(String bucketName, String prefix, String delimiter);

    /**
     * 生成预签名 URL (完整版)
     * @param bucketName 存储桶名称
     * @param key 对象键
     * @param expiration 过期时间
     * @param method HTTP方法
     * @param headers 请求头
     * @param params 请求参数
     * @param signPrefixMode 前缀签名模式
     * @param signHost 签名主机
     * @return 预签名URL
     */
    URL generatePresignedUrl(String bucketName, String key, Date expiration,
                             HttpMethodName method, Map<String, String> headers,
                             Map<String, String> params, Boolean signPrefixMode, Boolean signHost);

    /**
     * 生成用于上传的预签名URL
     * @param objectKey 对象键
     * @param contentType 文件类型
     * @param expireMinutes 过期分钟数
     * @return 预签名URL字符串
     */
    String generateUploadUrl(String objectKey, String contentType, Integer expireMinutes);

    /**
     * 生成带响应头覆盖的下载URL
     * @param objectKey 对象键
     * @param responseHeaders 响应头覆盖配置
     * @param expireMinutes 过期分钟数
     * @return 预签名URL字符串
     */
    String generateDownloadUrlWithResponseHeaders(String objectKey,
                                                  ResponseHeaderOverrides responseHeaders,
                                                  Integer expireMinutes);

    /**
     * 删除文件（单个）
     * @param bucketName 存储桶名称
     * @param objectKey 对象键
     */
    void deleteObject(String bucketName, String objectKey);

    /**
     * 删除文件（批量）
     * @param deleteObjectsRequest 批量删除请求
     * @return 删除结果
     */
    DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest);

    /**
     * 下载文件为字节数组
     * @param bucketName 存储桶名称
     * @param key 对象键
     * @return 文件内容字节数组
     */
    byte[] downloadFileAsBytes(String bucketName, String key);
}