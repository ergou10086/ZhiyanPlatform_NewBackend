package hbnu.project.zhiyanbackend.oss.controller;

import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.oss.dto.DeleteResultDTO;
import hbnu.project.zhiyanbackend.oss.dto.PresignedUrlResponseDTO;
import hbnu.project.zhiyanbackend.oss.dto.UploadFileResponseDTO;
import hbnu.project.zhiyanbackend.oss.service.COSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * COS 文件上传接口
 *
 * @author ErgouTree`
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/oss")
@RequiredArgsConstructor
@Tag(name = "对象存储", description = "基于腾讯云 COS 的文件操作")
public class COSController {

    @Resource
    private COSService cosService;

    /**
     * 简单接口：MultipartFile 上传
     * 参考腾讯云文档 <a href="https://cloud.tencent.com/document/product/436/65935#f1b2b774-d9cf-4645-8dea-b09a23045503">...</a>
     */
    @PostMapping("/simple")
    @Operation(summary = "简单上传（客户端文件）", description = "使用简单接口将 MultipartFile 直接上传至 COS")
    public R<UploadFileResponseDTO> uploadSimple(@RequestPart("file") MultipartFile file,
                                                 @RequestParam(value = "businessType", required = false) String businessType,
                                                 @RequestParam(value = "filename", required = false) String filename) {
        log.debug("收到简单上传请求, originalFilename={}, businessType={}", file.getOriginalFilename(), businessType);
        return R.ok(cosService.uploadFile(file, businessType, filename), "上传成功");
    }

    /**
     * 简单接口：服务端本地文件上传
     */
    @PostMapping("/simple/local")
    @Operation(summary = "简单上传（本地路径）", description = "后端读取本地文件并通过简单接口上传")
    public R<UploadFileResponseDTO> uploadSimpleFromLocal(@RequestParam("path") String localPath,
                                                          @RequestParam(value = "businessType", required = false) String businessType,
                                                          @RequestParam(value = "filename", required = false) String filename) {
        File file = new File(localPath);
        log.debug("收到本地文件简单上传请求, path={}, businessType={}", localPath, businessType);
        return R.ok(cosService.uploadLocalFile(file, businessType, filename), "上传成功");
    }

    /**
     * 高级接口：MultipartFile 上传，带进度日志
     * 参考 <a href="https://cloud.tencent.com/document/product/436/65935#2a08ffdc-0b58-4d33-a968-ea685d49c079">...</a>
     */
    @PostMapping("/advanced")
    @Operation(summary = "高级上传（客户端文件）", description = "使用 TransferManager 自动分块上传，并输出进度日志")
    public R<UploadFileResponseDTO> uploadAdvanced(@RequestPart("file") MultipartFile file,
                                                   @RequestParam(value = "businessType", required = false) String businessType,
                                                   @RequestParam(value = "filename", required = false) String filename) {
        log.debug("收到高级上传请求, originalFilename={}, businessType={}", file.getOriginalFilename(), businessType);
        return R.ok(cosService.uploadFileSenior(file, businessType, filename), "上传成功");
    }

    /**
     * 高级接口：服务器本地文件上传
     */
    @PostMapping("/advanced/local")
    @Operation(summary = "高级上传（本地路径）", description = "服务端通过 TransferManager 上传大文件，支持断点续传")
    public R<UploadFileResponseDTO> uploadAdvancedFromLocal(@RequestParam("path") String localPath,
                                                            @RequestParam(value = "businessType", required = false) String businessType,
                                                            @RequestParam(value = "filename", required = false) String filename) {
        File file = new File(localPath);
        log.debug("收到本地文件高级上传请求, path={}, businessType={}", localPath, businessType);
        return R.ok(cosService.uploadLocalFileSenior(file, businessType, filename), "上传成功");
    }

    /**
     * 高级接口：批量上传
     */
    @PostMapping("/advanced/batch")
    @Operation(summary = "高级上传（批量）", description = "批量上传多个文件，自动选择分块策略")
    public R<List<UploadFileResponseDTO>> uploadAdvancedBatch(@RequestPart("files") MultipartFile[] files,
                                                              @RequestParam(value = "businessType", required = false) String businessType) {
        int count = files == null ? 0 : files.length;
        log.debug("收到批量高级上传请求, fileCount={}, businessType={}", count, businessType);
        return R.ok(cosService.uploadBatch(files, businessType), "批量上传完成");
    }

    /**
     * 获取对象公网访问 URL
     */
    @GetMapping("/getObjectUrl")
    @Operation(summary = "获取对象访问 URL", description = "获取某个策略为公开访问的访问对象URL")
    public R<String> getObjectUrl(
            @RequestParam("objectKey") @Parameter(description = "对象键") String objectKey,
            @RequestParam(value = "bucketName", required = false) @Parameter(description = "桶名称，不传则使用默认桶") String bucketName
    ) {
        log.debug("获取对象URL: bucketName={}, objectKey={}", bucketName, objectKey);
        URL url = cosService.getObjectUrl(bucketName, objectKey);
        return R.ok(url.toString(), "获取成功");
    }

    /**
     * 生成预签名URL（通用）
     */
    @GetMapping("/generatePresignedUrl")
    @Operation(summary = "生成预签名URL", description = "生成临时访问URL，用于私有对象的临时访问")
    public R<PresignedUrlResponseDTO> generatePresignedUrl(
            @RequestParam("objectKey") @Parameter(description = "对象键") String objectKey,
            @RequestParam(value = "bucketName", required = false) @Parameter(description = "桶名称") String bucketName,
            @RequestParam(value = "expireMinutes", defaultValue = "60") @Parameter(description = "过期分钟数") Integer expireMinutes,
            @RequestParam(value = "method", defaultValue = "GET") @Parameter(description = "HTTP方法") String method) {
        log.debug("生成预签名URL: objectKey={}, expireMinutes={}, method={}", objectKey, expireMinutes, method);

        Date expiration = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L);
        HttpMethodName httpMethodName = HttpMethodName.valueOf(method.toUpperCase());

        URL url = cosService.generatePresignedUrl(bucketName, objectKey, expiration, httpMethodName, null, null, false, true);

        PresignedUrlResponseDTO response = PresignedUrlResponseDTO.builder()
                .url(url.toString())
                .objectKey(objectKey)
                .method(method)
                .expireMinutes(expireMinutes)
                .build();

        return R.ok(response, "生成成功");
    }

    /**
     * 生成预签名下载URL（带自定义文件名）
     */
    @GetMapping("/generateDownloadUrl")
    @Operation(summary = "生成下载URL", description = "生成带自定义文件名的下载URL")
    public R<PresignedUrlResponseDTO> generateDownloadUrl(
            @RequestParam("objectKey") @Parameter(description = "对象键") String objectKey,
            @RequestParam(value = "downloadFilename", required = false) @Parameter(description = "下载时的文件名") String downloadFilename,
            @RequestParam(value = "expireMinutes", defaultValue = "60") @Parameter(description = "过期分钟数") Integer expireMinutes) {
        log.debug("生成下载URL: objectKey={}, downloadFilename={}, expireMinutes={}",
                objectKey, downloadFilename, expireMinutes);

        ResponseHeaderOverrides responseHeaderOverrides = null;
        if (downloadFilename != null && !downloadFilename.isEmpty()) {
            responseHeaderOverrides = new ResponseHeaderOverrides();
            responseHeaderOverrides.setContentDisposition("attachment; filename=\"" + downloadFilename + "\"");
        }

        String url = cosService.generateDownloadUrlWithResponseHeaders(objectKey, responseHeaderOverrides, expireMinutes);

        PresignedUrlResponseDTO response = PresignedUrlResponseDTO.builder()
                .url(url)
                .objectKey(objectKey)
                .method("GET")
                .expireMinutes(expireMinutes)
                .downloadFilename(downloadFilename)
                .build();

        return R.ok(response, "生成成功");
    }

    /**
     * 列举对象
     */
    @GetMapping("/listObjects")
    @Operation(summary = "列举对象", description = "列举指定前缀下的对象")
    public R<List<String>> listObjects(
            @RequestParam(value = "prefix", defaultValue = "") @Parameter(description = "前缀") String prefix,
            @RequestParam(value = "bucketName", required = false) @Parameter(description = "桶名称") String bucketName,
            @RequestParam(value = "delimiter", required = false) @Parameter(description = "分隔符") String delimiter,
            @RequestParam(value = "maxKeys", defaultValue = "100") @Parameter(description = "最大返回数量") Integer maxKeys) {

        log.debug("列举对象: prefix={}, delimiter={}, maxKeys={}", prefix, delimiter, maxKeys);

        List<COSObjectSummary> objects = cosService.listObjectsByPrefix(bucketName, prefix, delimiter);
        List<String> objectKeys = objects.stream()
                .map(COSObjectSummary::getKey)
                .limit(maxKeys)
                .collect(Collectors.toList());

        return R.ok(objectKeys, "查询成功");
    }

    /**
     * 获取对象元数据
     */
    @GetMapping("/getObjectMetadata")
    @Operation(summary = "获取对象元数据", description = "获取对象的详细信息")
    public R<ObjectMetadata> getObjectMetadata(
            @RequestParam("objectKey") @Parameter(description = "对象键") String objectKey,
            @RequestParam(value = "bucketName", required = false) @Parameter(description = "桶名称") String bucketName) {

        log.debug("获取对象元数据: objectKey={}", objectKey);
        ObjectMetadata metadata = cosService.getObjectMetadata(bucketName, objectKey);
        return R.ok(metadata, "查询成功");
    }

    /**
     * 删除单个对象
     */
    @DeleteMapping("/deleteObject")
    @Operation(summary = "删除对象", description = "删除指定的单个对象")
    public R<Void> deleteObject(
            @RequestParam("objectKey") @Parameter(description = "对象键") String objectKey,
            @RequestParam(value = "bucketName", required = false) @Parameter(description = "桶名称") String bucketName) {

        log.debug("删除对象: objectKey={}", objectKey);
        cosService.deleteObject(bucketName, objectKey);
        return R.ok(null, "删除成功");
    }

    /**
     * 批量删除对象
     */
    @DeleteMapping("/deleteObjects")
    @Operation(summary = "批量删除对象", description = "批量删除多个对象")
    public R<DeleteResultDTO> deleteObjects(
            @RequestBody List<String> objectKeys,
            @RequestParam(value = "bucketName", required = false) @Parameter(description = "桶名称") String bucketName) {

        log.debug("批量删除对象: count={}", objectKeys.size());

        // 创建删除请求，直接传入 key 列表
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName)
                .withKeys(objectKeys.toArray(new String[0]));

        DeleteObjectsResult result = cosService.deleteObjects(request);

        // 构建响应DTO
        List<String> successKeys = result.getDeletedObjects().stream()
                .map(DeleteObjectsResult.DeletedObject::getKey)
                .collect(Collectors.toList());

        DeleteResultDTO responseDTO = DeleteResultDTO.builder()
                .totalCount(objectKeys.size())
                .successCount(successKeys.size())
                .failedCount(objectKeys.size() - successKeys.size())
                .successKeys(successKeys)
                .build();

        return R.ok(responseDTO, "批量删除完成");
    }
}


