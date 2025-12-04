package hbnu.project.zhiyanbackend.knowledge.controller;

import hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog;
import hbnu.project.zhiyanbackend.activelog.core.OperationLogHelper;
import hbnu.project.zhiyanbackend.activelog.model.enums.BizOperationModule;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.FileContextDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.UploadFileDTO;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementFileService;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * 成果文件上传接口
 * 负责把文件上传到成果中
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/zhiyan/achievement/file")    // 未修改
@Tag(name = "成果的文件管理", description = "成果文件上传等管理")
public class AchievementFileController {

    @Resource
    private AchievementFileService achievementFileService;

    @Resource
    private AchievementRepository achievementRepository;

    @Resource
    private OperationLogHelper operationLogHelper;


    /**
     * 上传成果文件
     * 上传单个文件到指定成果
     * 自带分片和断点续传
     */
    @PostMapping("/upload")
    @Operation(summary = "上传成果文件", description = "为指定成果上传单个文件")
    @BizOperationLog(module = BizOperationModule.ACHIEVEMENT, type = "FILE_UPLOAD", description = "上传文件")
    public R<AchievementFileDTO> uploadFile(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "成果ID") @RequestParam("achievementId") Long achievementId){

        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("上传成果文件: achievementId={}, fileName={}, size={}, userId={}",
                achievementId, file.getOriginalFilename(), file.getSize(), userId);

        UploadFileDTO uploadDTO = UploadFileDTO.builder()
                .achievementId(achievementId)
                .uploadBy(userId)
                .build();

        // 执行文件上传
        AchievementFileDTO fileDTO = achievementFileService.uploadFile(file, uploadDTO);

        // 操作成功后记录日志
        if (fileDTO != null) {
            achievementRepository.findById(achievementId).ifPresent(achievement -> operationLogHelper.logAchievementFileUpload(
                    achievement.getProjectId(),
                    achievementId,
                    achievement.getTitle(),
                    file.getOriginalFilename(),
                    file.getSize()
            ));
        }

        log.info("文件上传成功: fileId={}, url={}", Objects.requireNonNull(fileDTO).getId(), fileDTO.getFileUrl());
        return R.ok(fileDTO, "文件上传成功");
    }


    /**
     * 批量上传成果文件
     * 批量上传多个文件到指定成果
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传成果文件", description = "为指定成果批量上传多个文件，使用COS批量上传功能")
    @BizOperationLog(module = BizOperationModule.ACHIEVEMENT, type = "FILE_UPLOAD", description = "批量上传文件")
    public R<List<AchievementFileDTO>> uploadFilesBatch(
            @Parameter(description = "文件列表") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "成果ID") @RequestParam("achievementId") Long achievementId){

        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("批量上传成果文件: achievementId={}, fileCount={}, userId={}", 
                achievementId, files != null ? files.length : 0, userId);
        if (files == null || files.length == 0) {
            return R.fail("文件列表不能为空");
        }

        // 执行批量上传
        List<AchievementFileDTO> fileDTOs = achievementFileService.uploadFilesBatch(files, achievementId, userId);

        // 操作成功后记录日志
        if (fileDTOs != null && !fileDTOs.isEmpty()) {
            achievementRepository.findById(achievementId).ifPresent(achievement -> operationLogHelper.logAchievementBatchFileUpload(
                    achievement.getProjectId(),
                    achievementId,
                    achievement.getTitle(),
                    fileDTOs.size()
            ));
        }

        log.info("批量上传成果文件成功: achievementId={}, 成功上传 {} 个文件", 
                achievementId, Objects.requireNonNull(fileDTOs).size());
        return R.ok(fileDTOs, String.format("成功上传 %d 个文件", fileDTOs.size()));
    }

    /**
     * 查询成果的所有文件
     * 获取指定成果下的所有文件列表
     */
    @GetMapping("/{achievementId}/files")
    @Operation(summary = "查询成果文件列表", description = "获取指定成果下的所有文件")
    public R<List<AchievementFileDTO>> getAchievementFiles(
            @Parameter(description = "成果ID") @PathVariable Long achievementId) {

        log.info("查询成果文件列表: achievementId={}", achievementId);

        List<AchievementFileDTO> files = achievementFileService.getFilesByAchievementId(achievementId);

        return R.ok(files, "查询成功");
    }

    /**
     * 删除成果文件
     * 删除指定的文件
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除成果文件", description = "删除指定的成果文件")
    @BizOperationLog(module = BizOperationModule.ACHIEVEMENT, type = "FILE_DELETE", description = "删除文件")
    public R<Void> deleteAchievementFile(
            @Parameter(description = "文件ID")  @PathVariable Long fileId){
        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("删除成果文件: fileId={}, userId={}", fileId, userId);

        // 获取文件信息（用于日志）
        AchievementFileDTO fileDTO = achievementFileService.getFileById(fileId);
        Long achievementId = fileDTO.getAchievementId();
        String fileName = fileDTO.getFileName();

        achievementFileService.deleteFile(fileId, userId);

        // 操作成功后记录日志
        achievementRepository.findById(achievementId).ifPresent(achievement -> operationLogHelper.logAchievementFileDelete(
                achievement.getProjectId(),
                achievementId,
                achievement.getTitle(),
                fileName
        ));

        log.info("文件删除成功: fileId={}", fileId);
        return R.ok(null, "文件删除成功");
    }

    /**
     * 批量删除成果文件
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除成果文件", description = "批量删除指定的成果文件")
    @BizOperationLog(module = BizOperationModule.ACHIEVEMENT, type = "FILE_DELETE", description = "批量删除文件")
    public R<Void> deleteFilesBatch(
            @Parameter(description = "文件ID列表") @RequestParam("fileIds") List<Long> fileIds) {
        Long userId = SecurityUtils.getUserId();
        log.info("批量删除成果文件: fileIds={}, userId={}", fileIds, userId);
        if (fileIds == null || fileIds.isEmpty()) {
            return R.fail("文件ID列表不能为空");
        }

        // 获取第一个文件的成果信息（用于日志，因为批量删除的都是同一成果的文件）
        AchievementFileDTO firstFile = achievementFileService.getFileById(fileIds.getFirst());
        Long achievementId = firstFile.getAchievementId();

        // 执行批量删除
        achievementFileService.deleteFiles(fileIds, userId);

        // 操作成功后记录日志
        achievementRepository.findById(achievementId).ifPresent(achievement -> operationLogHelper.logAchievementBatchFileDelete(
                achievement.getProjectId(),
                achievementId,
                achievement.getTitle(),
                fileIds.size()
        ));

        return R.ok(null, "批量删除成功");
    }

    /**
     * 获取文件预签名 URL
     * 用于下载
     */
    @GetMapping("/{fileId}/download-url")
    @Operation(summary = "获取文件预签名URL", description = "生成文件的临时下载链接")
    public R<String> getFileDownloadUrl(
            @Parameter(description = "文件ID") @PathVariable Long fileId,
            @Parameter(description = "过期时间(秒)") @RequestParam(defaultValue = "3600") Integer expirySeconds) {
        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        log.info("获取文件预签名URL: fileId={}, userId={}, expirySeconds={}", fileId, userId, expirySeconds);

        // 检查用户是否已登录
        if (userId == null) {
            log.error("用户未登录或token失效: fileId={}", fileId);
            return R.fail(401, "用户未登录或登录已过期，请重新登录");
        }

        try {
            String downloadUrl = achievementFileService.generatePresignedUrl(fileId, userId, expirySeconds);
            log.info("文件预签名URL生成成功: fileId={}, userId={}", fileId, userId);
            return R.ok(downloadUrl, "预签名URL生成成功");
        } catch (ControllerException e) {
            log.error("获取文件预签名URL失败: fileId={}, userId={}, error={}", fileId, userId, e.getMessage(), e);
            return R.fail(500, "获取预签名URL失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件ID查询单个文件信息
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "查询文件信息", description = "根据文件ID查询文件详细信息")
    public R<AchievementFileDTO> getFileInfo(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        log.info("查询文件信息: fileId={}", fileId);

        try{
            AchievementFileDTO fileDTO = achievementFileService.getFileById(fileId);
            return R.ok(fileDTO, "查询成功");
        }catch (ControllerException e){
            log.error("查询文件信息失败: fileId={}", fileId, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 统计成果的文件数量
     */
    @GetMapping("/{achievementId}/count")
    @Operation(summary = "统计成果文件数量", description = "统计指定成果下的文件数量")
    public R<Long> countAchievementFiles(
            @Parameter(description = "成果ID") @PathVariable Long achievementId) {
        log.info("统计成果文件数量: achievementId={}", achievementId);

        long count = achievementFileService.countFilesByAchievementId(achievementId);
        return R.ok(count, String.format("该成果共有 %d 个文件", count));
    }

    /**
     * 验证文件访问权限
     */
    @GetMapping("/{fileId}/permission")
    @Operation(summary = "验证文件权限", description = "验证当前用户是否有权限访问指定文件")
    public R<Boolean> checkFilePermission(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {

        Long userId = SecurityUtils.getUserId();
        log.info("验证文件权限: fileId={}, userId={}", fileId, userId);

        boolean hasPermission = achievementFileService.hasFilePermission(fileId, userId);
        return R.ok(hasPermission, hasPermission ? "有访问权限" : "无访问权限");
    }

    /**
     * 根据文件 ID 获取文件上下文信息
     * 该接口大多数是给AI使用
     *
     * @param fileId 文件 ID
     * @return 文件上下文
     */
    @GetMapping("/content/{fileId}")
    @Operation(summary = "获取单个文件信息", description = "根据文件 ID 获取文件的详细信息")
    public R<FileContextDTO> getFileById(
            @Parameter(description = "文件 ID") @PathVariable Long fileId) {
        log.info("[文件查询] 获取文件: fileId={}", fileId);

        FileContextDTO fileContext = achievementFileService.getFileContext(fileId);

        if (fileContext == null) {
            return R.fail("文件不存在");
        }

        return R.ok(fileContext);
    }

    /**
     * 批量获取文件上下文信息
     *
     * @param fileIds 文件 ID 列表
     * @return 文件上下文列表
     */
    @GetMapping("/batch")
    @Operation(summary = "批量获取文件信息", description = "根据文件 ID 列表批量获取文件信息")
    public R<List<FileContextDTO>> getFilesByIds(
            @Parameter(description = "文件 ID 列表") @RequestParam("fileIds") List<Long> fileIds) {
        log.info("[文件批量查询] 获取文件: fileIds={}, count={}", fileIds, fileIds.size());

        List<FileContextDTO> fileContexts = achievementFileService.getFileContexts(fileIds);

        return R.ok(fileContexts, String.format("成功获取 %d 个文件", fileContexts.size()));
    }
}
