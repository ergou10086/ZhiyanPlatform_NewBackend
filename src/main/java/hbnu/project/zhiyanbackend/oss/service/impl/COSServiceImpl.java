package hbnu.project.zhiyanbackend.oss.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
import com.qcloud.cos.transfer.Transfer;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferProgress;
import com.qcloud.cos.transfer.Upload;
import hbnu.project.zhiyanbackend.oss.config.COSProperties;
import hbnu.project.zhiyanbackend.oss.dto.UploadFileResponseDTO;
import hbnu.project.zhiyanbackend.oss.exception.OssException;
import hbnu.project.zhiyanbackend.oss.service.COSService;
import hbnu.project.zhiyanbackend.oss.utils.COSUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 腾讯云 COS 对象存储服务
 * 目前该架构设置为单桶策略，桶内设置文件夹区分业务
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class COSServiceImpl implements COSService {

    private static final long PROGRESS_LOG_INTERVAL_MS = 2000L;

    private final COSClient cosClient;
    private final COSProperties cosProperties;
    private final COSUtils cosUtils;
    @Resource
    private TransferManager transferManager;

    /**
     * 上传文件到默认桶，使用上传流
     * 简单接口 <a href="https://cloud.tencent.com/document/product/436/65935#f1b2b774-d9cf-4645-8dea-b09a23045503">...</a>，上传 MultipartFile 文件到 COS
     */
    @Override
    public UploadFileResponseDTO uploadFile(MultipartFile file, String businessType, String filename) {
        if (file == null || file.isEmpty()) {
            throw new OssException("文件不能为空");
        }

        // 构建对象键和元数据
        String objectKey = cosUtils.buildObjectKey(businessType, filename, file.getOriginalFilename());
        ObjectMetadata metadata = buildMetadataFromMultipartFile(file);

        // 上传流类型
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(cosProperties.getBucketName(), objectKey, inputStream, metadata);
            PutObjectResult result = cosClient.putObject(request);

            log.info("COS 文件上传成功: bucket={}, key={}, size={}", cosProperties.getBucketName(), objectKey, file.getSize());

            return buildUploadResponse(objectKey, result, file.getOriginalFilename(),
                    file.getSize(), metadata.getContentType());
        } catch (CosClientException e) {
            log.error("COS 文件上传失败（服务端）: {}", e.getMessage(), e);
            throw new OssException("腾讯云COS上传失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("COS 文件上传失败（IO）", e);
            throw new OssException("文件读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传本地文件到COS，本地文件上传形式
     * 简单接口 <a href="https://cloud.tencent.com/document/product/436/65935#f1b2b774-d9cf-4645-8dea-b09a23045503">...</a>，上传 MultipartFile 文件到 COS
     */
    @Override
    public UploadFileResponseDTO uploadLocalFile(File localFile, String businessType, String filename) {
        // 参数校验
        validateLocalFile(localFile);

        // 构建对象键和元数据
        String originalFilename = localFile.getName();
        String objectKey = cosUtils.buildObjectKey(businessType, filename, localFile.getName());
        ObjectMetadata metadata = buildMetadataFromFile(localFile);

        try{
            // 使用 File 对象直接上传 (COS SDK 会自动处理文件流)
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosProperties.getBucketName(), objectKey, localFile);
            // 设置存储类型（如有需要，不需要请忽略此行代码）, 默认是标准(Standard), 低频(standard_ia)
            // 更多存储类型请参见 https://cloud.tencent.com/document/product/436/33417
            putObjectRequest.setStorageClass(StorageClass.Standard_IA);
            // 设置元数据
            putObjectRequest.setMetadata(metadata);
            PutObjectResult result = cosClient.putObject(putObjectRequest);

            log.info("COS 本地文件上传成功: bucket={}, key={}, localPath={}, size={}",
                    cosProperties.getBucketName(), objectKey, localFile.getAbsolutePath(), localFile.length());

            return buildUploadResponse(objectKey, result, originalFilename,
                    localFile.length(), metadata.getContentType());
        } catch (CosServiceException e) {
            log.error("COS 本地文件上传失败(服务端): statusCode={}, errorCode={}, errorMessage={}, localPath={}",
                    e.getStatusCode(), e.getErrorCode(), e.getErrorMessage(), localFile.getAbsolutePath(), e);
            throw new OssException("腾讯云 COS 上传失败: " + e.getErrorMessage(), e);
        } catch (CosClientException e) {
            log.error("COS 本地文件上传失败(客户端): {}, localPath={}",
                    e.getMessage(), localFile.getAbsolutePath(), e);
            throw new OssException("COS 客户端错误: " + e.getMessage(), e);
        }
    }

    /**
     * 上传 MultipartFile 文件，使用对象流
     * 高级接口，<a href="https://cloud.tencent.com/document/product/436/65935#2a08ffdc-0b58-4d33-a968-ea685d49c079">...</a>
     * 自动判断是否使用分块上传，带进度监听
     *
     * @param file MultipartFile 文件
     * @param businessType 业务类型（avatar/document/product等）
     * @param filename 自定义文件名（可选，为 null 则自动生成）
     * @return 上传结果
     */
    @Override
    public UploadFileResponseDTO uploadFileSenior(MultipartFile file, String businessType, String filename) {
        validateMultipartFile(file);

        // 构建对象键，包含业务类型目录
        String objectKey = cosUtils.buildObjectKey(businessType, filename, file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            // 构建元数据
            ObjectMetadata metadata = buildMetadataFromMultipartFile(file);

            // 创建上传请求
            PutObjectRequest request = new PutObjectRequest(cosProperties.getBucketName(), objectKey, inputStream, metadata);

            // 设置存储类型（低频存储）
            request.setStorageClass(StorageClass.Standard_IA);

            // 启用断点续传
            if (cosProperties.getTransfer().isEnableResumable()) {
                request.setEnableResumableUpload(true);
            }

            // 使用 TransferManager 上传
            Upload upload = transferManager.upload(request);
            monitorTransferProgress(upload, objectKey);

            // 等待上传返回结果
            UploadResult result = upload.waitForUploadResult();

            log.info("文件上传成功: bucket={}, key={}, size={} bytes, businessType={}",
                    cosProperties.getBucketName(), objectKey, file.getSize(), businessType);

            // 构建高级接口用的结果
            return buildUploadResponseSenior(objectKey, result, file.getOriginalFilename(),
                    file.getSize(), metadata.getContentType());
        } catch (CosServiceException e) {
            log.error("COS 上传失败(服务端): statusCode={}, errorCode={}, key={}",
                    e.getStatusCode(), e.getErrorCode(), objectKey, e);
            throw new OssException("腾讯云 COS 上传失败: " + e.getErrorMessage(), e);
        } catch (CosClientException e) {
            log.error("COS 上传失败(客户端): {}, key={}", e.getMessage(), objectKey, e);
            throw new OssException("COS 客户端错误: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("COS 上传被中断: key={}", objectKey, e);
            throw new OssException("文件上传被中断", e);
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
            throw new OssException("文件读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传本地文件
     * 高级接口，<a href="https://cloud.tencent.com/document/product/436/65935#2a08ffdc-0b58-4d33-a968-ea685d49c079">...</a>
     * 支持大文件分块上传、断点续传
     *
     * @param localFile 本地文件对象
     * @param businessType 业务类型
     * @param filename 自定义文件名（可选）
     * @return 上传结果
     */
    @Override
    public UploadFileResponseDTO uploadLocalFileSenior(File localFile, String businessType, String filename) {
        validateLocalFile(localFile);

        // 构建对象键
        String objectKey = cosUtils.buildObjectKey(businessType, filename, localFile.getName());
        ObjectMetadata metadata = buildMetadataFromFile(localFile);

        try {
            // 创建上传请求
            PutObjectRequest request = new PutObjectRequest(cosProperties.getBucketName(), objectKey, localFile);

            // 设置存储类型和元数据
            request.setStorageClass(StorageClass.Standard_IA);
            request.setMetadata(metadata);

            // 启用断点续传
            if (cosProperties.getTransfer().isEnableResumable()) {
                request.setEnableResumableUpload(true);
            }

            // 使用 TransferManager 上传
            Upload upload = transferManager.upload(request);
            monitorTransferProgress(upload, objectKey);

            // 等待上传完成返回结果
            UploadResult result = upload.waitForUploadResult();

            log.info("本地文件上传成功: bucket={}, key={}, size={} bytes, businessType={}",
                    cosProperties.getBucketName(), objectKey, localFile.length(), businessType);

            return buildUploadResponseSenior(objectKey, result, localFile.getName(),
                    localFile.length(), metadata.getContentType());
        }catch (CosServiceException e) {
            log.error("COS 本地文件上传失败(服务端): statusCode={}, errorCode={}, key={}",
                    e.getStatusCode(), e.getErrorCode(), objectKey, e);
            throw new OssException("腾讯云 COS 上传失败: " + e.getErrorMessage(), e);
        } catch (CosClientException e) {
            log.error("COS 本地文件上传失败(客户端): {}, key={}", e.getMessage(), objectKey, e);
            throw new OssException("COS 客户端错误: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("COS 上传被中断: key={}", objectKey, e);
            throw new OssException("文件上传被中断", e);
        }
    }

    /**
     * 批量上传文件
     * 调用上传本地文件的高级接口
     *
     * @param files 文件数组
     * @param businessType 业务类型
     * @return 上传结果列表
     */
    @Override
    public List<UploadFileResponseDTO> uploadBatch(MultipartFile[] files, String businessType) {
        if (files == null || files.length == 0) {
            throw new OssException("文件列表不能为空");
        }

        List<UploadFileResponseDTO> results = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                UploadFileResponseDTO result = uploadFileSenior(file, businessType, null);
                results.add(result);
            } catch (Exception e) {
                log.error("批量上传中某个文件失败: filename={}, error={}",
                        file.getOriginalFilename(), e.getMessage(), e);
                // 继续上传其他文件，不中断整个批量上传过程
            }
        }

        log.info("批量上传完成: total={}, success={}, businessType={}",
                files.length, results.size(), businessType);

        return results;
    }

    /**
     * 列出目录下的对象和子目录
     * <a href="https://cloud.tencent.com/document/product/436/65938#cbc72651-5be1-4a12-9b97-88cb3ecd1f9c">...</a>
     * 用于指定业务对象的枚举
     */
    @Override
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest){
        try{
            // 执行列举操作
            ObjectListing objectListing = cosClient.listObjects(listObjectsRequest);

            // 获取对象摘要列表
            List<COSObjectSummary> objectSummaries = objectListing.getObjectSummaries();

            log.info("COS对象列举成功: bucket={}, prefix={}, count={}, isTruncated={}",
                    listObjectsRequest.getBucketName(),
                    listObjectsRequest.getPrefix(),
                    objectSummaries != null ? objectSummaries.size() : 0,
                    objectListing.isTruncated());

            // 输出公共前缀(子目录)
            List<String> commonPrefixes = objectListing.getCommonPrefixes();
            if (commonPrefixes != null && !commonPrefixes.isEmpty()) {
                log.debug("子目录列表: {}", commonPrefixes);
            }

            return objectListing;
        }catch (CosServiceException e) {
            log.error("COS列举对象失败(服务端): statusCode={}, errorCode={}, errorMessage={}",
                    e.getStatusCode(), e.getErrorCode(), e.getErrorMessage(), e);
            throw new OssException("列举对象失败: " + e.getErrorMessage(), e);
        } catch (CosClientException e) {
            log.error("COS列举对象失败(客户端): {}", e.getMessage(), e);
            throw new OssException("COS客户端错误: " + e.getMessage(), e);
        }
    }

    /**
     * 列举指定前缀的所有对象
     * 预留特殊接口
     */
    @Override
    public List<COSObjectSummary> listObjectsByPrefix(String bucketName, String prefix, String delimiter) {
        String targetBucket = StringUtils.isNotBlank(bucketName)
                ? bucketName
                : cosProperties.getBucketName();

        List<COSObjectSummary> allObjects = new ArrayList<>();
        String nextMarker = null;
        boolean isTruncated = true;

        try{
            // 循环列举所有对象
            while (isTruncated) {
                ListObjectsRequest listRequest = new ListObjectsRequest();
                listRequest.setBucketName(targetBucket);
                listRequest.setPrefix(prefix);

                if (StringUtils.isNotBlank(delimiter)) {
                    listRequest.setDelimiter(delimiter);
                }

                // 设置每次最多列举1000个
                listRequest.setMaxKeys(1000);

                if (nextMarker != null) {
                    listRequest.setMarker(nextMarker);
                }

                ObjectListing objectListing = listObjects(listRequest);

                // 添加当前批次的对象
                if(objectListing.getObjectSummaries() != null && !objectListing.getObjectSummaries().isEmpty()) {
                    allObjects.addAll(objectListing.getObjectSummaries());
                }

                // 检查是否还有更多对象
                nextMarker = objectListing.getNextMarker();
                isTruncated = objectListing.isTruncated();
            }

            log.info("列举完成: bucket={}, prefix={}, totalCount={}",
                    targetBucket, prefix, allObjects.size());

            return allObjects;
        }catch (CosServiceException e) {
            log.error("列举对象失败: bucket={}, prefix={}", targetBucket, prefix, e);
            throw new OssException("列举对象失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询指定对象的元数据信息，HEAD Object
     * <a href="https://cloud.tencent.com/document/product/436/65941#c405a320-689d-42b1-9faf-63db0bffd208">...</a>
     */
    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key){
        try {
            // 使用默认桶名(如果未指定)
            String targetBucket = StringUtils.isNotBlank(bucketName)
                    ? bucketName
                    : cosProperties.getBucketName();

            // 查询对象元数据
            ObjectMetadata objectMetadata = cosClient.getObjectMetadata(bucketName, key);

            log.info("查询对象元数据成功: bucket={}, key={}, contentType={}, contentLength={}, lastModified={}",
                    targetBucket,
                    key,
                    objectMetadata.getContentType(),
                    objectMetadata.getContentLength(),
                    objectMetadata.getLastModified());

            return objectMetadata;
        }catch (CosClientException e) {
            log.error("查询对象元数据失败(客户端): {}, key={}", e.getMessage(), key, e);
            throw new OssException("COS客户端错误: " + e.getMessage(), e);
        }
    }

    /**
     * 获取对象访问 URL
     * <a href="https://cloud.tencent.com/document/product/436/57316#18b75fd0-a3e5-4099-b871-921095c4fd61">...</a>
     * 用于公开读的对象
     */
    @Override
    public URL getObjectUrl(String bucketName, String key){
        try {
            String targetBucket = StringUtils.isNotBlank(bucketName)
                    ? bucketName
                    : cosProperties.getBucketName();

            URL url = cosClient.getObjectUrl(targetBucket, key);

            log.debug("获取对象URL: bucket={}, key={}, url={}", targetBucket, key, url);

            return url;
        } catch (Exception e) {
            log.error("获取对象URL失败: bucket={}, key={}", bucketName, key, e);
            throw new OssException("获取对象URL失败", e);
        }
    }

    /**
     * 生成预签名 URL
     * <a href="https://cloud.tencent.com/document/product/436/35217#a9387c63-65e9-44de-bd4b-2819516a7796">...</a>
     * 使用该预签名URL下载
     */
    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethodName method, Map<String, String> headers, Map<String, String> params, Boolean signPrefixMode, Boolean signHost){
        try{
            // 使用默认桶名(如果未指定)
            String targetBucket = StringUtils.isNotBlank(bucketName)
                    ? bucketName
                    : cosProperties.getBucketName();

            // 请求的 HTTP 方法，上传请求用 PUT，下载请求用 GET，删除请求用 DELETE，默认是GET
            GeneratePresignedUrlRequest request =  new GeneratePresignedUrlRequest(targetBucket, key, method != null ? method : HttpMethodName.GET);

            // 设置过期时间(如果未指定,默认1小时后过期)
            if (expiration != null) {
                request.setExpiration(expiration);
            } else {
                Date defaultExpiration = new Date(System.currentTimeMillis() + 60 * 60 * 1000);
                request.setExpiration(defaultExpiration);
            }

            // 填写本次请求的头部，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的头部
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(request::putCustomRequestHeader);
            }

            // 填写本次请求的参数，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的参数
            if (params != null && !params.isEmpty()) {
                params.forEach(request::addRequestParameter);
            }

            // 设置是否以前缀方式签名(不推荐)
            if (signPrefixMode != null) {
                request.setSignPrefixMode(signPrefixMode);
            }

            // 生成预签名URL
            URL url = cosClient.generatePresignedUrl(request);

            log.info("生成预签名URL成功: bucket={}, key={}, method={}, expiration={}",
                    targetBucket, key, method, expiration);

            return url;
        }catch (CosServiceException e) {
            log.error("生成预签名URL失败(服务端): statusCode={}, errorCode={}, key={}",
                    e.getStatusCode(), e.getErrorCode(), key, e);
            throw new OssException("生成预签名URL失败: " + e.getErrorMessage(), e);
        } catch (CosClientException e) {
            log.error("生成预签名URL失败(客户端): {}, key={}", e.getMessage(), key, e);
            throw new OssException("COS客户端错误: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名上传 URL 并使用该 URL 上传对象
     * <a href="https://cloud.tencent.com/document/product/436/35217#eedb4a52-df02-4c2f-aa9a-b1f36e142236">...</a>
     * 预留服务方法，不特殊情况下就不使用
     */
    @Override
    public String generateUploadUrl(String objectKey, String contentType, Integer expireMinutes) {
        try {
            int minutes = (expireMinutes != null && expireMinutes > 0) ? expireMinutes : 30;
            Date expiration = new Date(System.currentTimeMillis() + minutes * 60 * 1000L);

            // 设置Content-Type头部
            Map<String, String> headers = null;
            if (StringUtils.isNotBlank(contentType)) {
                headers = new HashMap<>();
                headers.put("Content-Type", contentType);
            }

            URL url = generatePresignedUrl(
                    cosProperties.getBucketName(),
                    objectKey,
                    expiration,
                    HttpMethodName.PUT,
                    headers,
                    null,
                    false,
                    true
            );

            log.info("生成上传预签名URL: key={}, contentType={}, expireMinutes={}",
                    objectKey, contentType, minutes);

            return url.toString();
        }catch (CosServiceException e) {
            log.error("生成上传URL失败: key={}", objectKey, e);
            throw new OssException("生成上传URL失败", e);
        }
    }

    /**
     * 生成覆盖返回头部的预签名下载URL
     * 用于自定义下载时的文件名、内容类型等
     * <a href="https://cloud.tencent.com/document/product/436/35217#8afa088c-b0f9-40f6-9729-3ad08514a020">...</a>
     * 预留服务方法，不特殊情况下就不使用
     */
    @Override
    public String generateDownloadUrlWithResponseHeaders(String objectKey, ResponseHeaderOverrides responseHeaders, Integer expireMinutes) {
        try{
            int minutes = (expireMinutes != null && expireMinutes > 0) ? expireMinutes : 60;
            Date expiration = new Date(System.currentTimeMillis() + minutes * 60 * 1000L);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    cosProperties.getBucketName(),
                    objectKey,
                    HttpMethodName.GET
            );
            request.setExpiration(expiration);

            // 设置响应头覆盖
            if (responseHeaders != null) {
                request.setResponseHeaders(responseHeaders);
            }

            URL url = cosClient.generatePresignedUrl(request);

            log.info("生成带响应头覆盖的下载URL: key={}, expireMinutes={}", objectKey, minutes);

            return url.toString();
        }catch (CosServiceException e) {
            log.error("生成带响应头覆盖的下载URL失败: key={}", objectKey, e);
            throw new OssException("生成下载URL失败", e);
        }
    }

    /**
     * 删除文件
     * 单个删除，<a href="https://cloud.tencent.com/document/product/436/65939">...</a>
     */
    @Override
    public void deleteObject(String bucketName, String objectKey){
        try {
            // 使用默认桶名(如果未指定)
            String targetBucket = StringUtils.isNotBlank(bucketName)
                    ? bucketName
                    : cosProperties.getBucketName();

            // 调用COS SDK删除对象
            cosClient.deleteObject(targetBucket, objectKey);

            log.info("COS对象删除成功: bucket={}, key={}", targetBucket, objectKey);

        } catch (CosServiceException e) {
            log.error("COS删除对象失败(服务端): statusCode={}, errorCode={}, errorMessage={}, key={}",
                    e.getStatusCode(), e.getErrorCode(), e.getErrorMessage(), objectKey, e);
            throw new OssException("删除文件失败: " + e.getErrorMessage(), e);
        } catch (CosClientException e) {
            log.error("COS删除对象失败(客户端): {}, key={}", e.getMessage(), objectKey, e);
            throw new OssException("COS客户端错误: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     * 批量删除，<a href="https://cloud.tencent.com/document/product/436/65939#841fe310-bdf8-4789-9bc0-26ea844e316d">...</a>
     */
    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest){
        try{
            // 执行批量删除
            DeleteObjectsResult result = cosClient.deleteObjects(deleteObjectsRequest);

            // 获取删除成功和失败的对象列表
            List<DeleteObjectsResult.DeletedObject> deletedObjects = result.getDeletedObjects();

            log.info("COS批量删除完成: bucket={}, 成功数量={}",
                    deleteObjectsRequest.getBucketName(),
                    deletedObjects != null ? deletedObjects.size() : 0);

            // 如果有删除失败的对象,记录错误信息
            if (result.getDeletedObjects() != null && !result.getDeletedObjects().isEmpty()) {
                assert deletedObjects != null;
                log.debug("删除成功的对象: {}",
                        deletedObjects.stream()
                                .map(DeleteObjectsResult.DeletedObject::getKey)
                                .toList());
            }

            return result;
        } catch (MultiObjectDeleteException mde) {
            // 部分删除成功,部分失败
            List<DeleteObjectsResult.DeletedObject> deletedObjects = mde.getDeletedObjects();
            List<MultiObjectDeleteException.DeleteError> deleteErrors = mde.getErrors();

            log.warn("COS批量删除部分失败: 成功={}, 失败={}",
                    deletedObjects != null ? deletedObjects.size() : 0,
                    deleteErrors != null ? deleteErrors.size() : 0);

            // 记录失败的对象
            if (deleteErrors != null && !deleteErrors.isEmpty()) {
                deleteErrors.forEach(error ->
                        log.error("删除失败: key={}, code={}, message={}",
                                error.getKey(), error.getCode(), error.getMessage())
                );
            }

            throw new OssException("批量删除部分失败: " +
                    (deleteErrors != null ? deleteErrors.size() : 0) + " 个对象删除失败", mde);

        } catch (CosServiceException e) {
            log.error("COS批量删除失败(服务端): statusCode={}, errorCode={}, errorMessage={}",
                    e.getStatusCode(), e.getErrorCode(), e.getErrorMessage(), e);
            throw new OssException("批量删除文件失败: " + e.getErrorMessage(), e);
        } catch (CosClientException e) {
            log.error("COS批量删除失败(客户端): {}", e.getMessage(), e);
            throw new OssException("COS客户端错误: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 MultipartFile 的对象元数据
     * <p>
     * ObjectMetadata 用于描述 COS 对象的属性,包括:
     * - Content-Length: 文件大小,用于传输验证
     * - Content-Type: MIME 类型,影响浏览器如何处理文件
     * - Content-Disposition: 下载时的文件名设置
     */
    private ObjectMetadata buildMetadataFromMultipartFile(MultipartFile file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(cosUtils.resolveContentType(file.getContentType(), file.getOriginalFilename()));
        metadata.setContentDisposition(cosUtils.buildContentDisposition(file.getOriginalFilename()));
        return metadata;
    }

    /**
     * 构建本地 File 对象的元数据
     */
    private ObjectMetadata buildMetadataFromFile(File file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());
        metadata.setContentType(cosUtils.resolveContentType(null, file.getName()));
        metadata.setContentDisposition(cosUtils.buildContentDisposition(file.getName()));
        return metadata;
    }

    /**
     * 构建统一的上传响应对象
     * 简单接口
     *
     * @param objectKey COS 对象键
     * @param result COS 上传结果
     * @param originalFilename 原始文件名
     * @param size 文件大小
     * @param contentType 文件类型
     * @return 上传响应 DTO
     */
    private UploadFileResponseDTO buildUploadResponse(String objectKey, PutObjectResult result,
                                                      String originalFilename, long size, String contentType) {
        return UploadFileResponseDTO.builder()
                .objectKey(objectKey)
                .url(cosUtils.buildPublicUrl(objectKey))
                .eTag(result.getETag())
                .originalFilename(originalFilename)
                .size(size)
                .contentType(contentType)
                .build();
    }

    /**
     * 构建统一的上传响应对象
     * 高级接口
     */
    private UploadFileResponseDTO buildUploadResponseSenior(String objectKey, UploadResult result,
                                                            String originalFilename, long size, String contentType) {
        return UploadFileResponseDTO.builder()
                .objectKey(objectKey)
                .url(cosUtils.buildPublicUrl(objectKey))
                .eTag(result.getETag())
                .originalFilename(originalFilename)
                .size(size)
                .contentType(contentType)
                .build();
    }

    /**
     * 校验 MultipartFile
     */
    private void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OssException("文件不能为空");
        }
    }

    /**
     * 校验本地文件
     */
    private void validateLocalFile(File localFile) {
        if (localFile == null) {
            throw new OssException("文件对象不能为空");
        }
        if (!localFile.exists()) {
            throw new OssException("文件不存在: " + localFile.getAbsolutePath());
        }
        if (!localFile.isFile()) {
            throw new OssException("路径不是一个文件: " + localFile.getAbsolutePath());
        }
        if (!localFile.canRead()) {
            throw new OssException("文件无法读取: " + localFile.getAbsolutePath());
        }
    }

    /**
     * 实时输出上传进度
     * 该方法不会阻塞上传主流程，只用于日志观察和后续扩展。
     */
    private void monitorTransferProgress(Transfer transfer, String objectKey) {
        CompletableFuture.runAsync(() -> {
            log.info("开始监控 COS 上传进度: key={}, desc={}", objectKey, transfer.getDescription());
            try {
                while (!transfer.isDone()) {
                    TransferProgress progress = transfer.getProgress();
                    long uploaded = progress.getBytesTransferred();
                    long total = progress.getTotalBytesToTransfer();
                    double pct = progress.getPercentTransferred();
                    log.info("COS 上传进度: key={}, {}/{} bytes ({}%)",
                            objectKey, uploaded, total, String.format("%.2f", pct));
                    Thread.sleep(PROGRESS_LOG_INTERVAL_MS);
                }
                log.info("COS 上传状态: key={}, state={}", objectKey, transfer.getState());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("COS 上传进度监控被中断: key={}", objectKey, e);
            }
        });
    }
}

