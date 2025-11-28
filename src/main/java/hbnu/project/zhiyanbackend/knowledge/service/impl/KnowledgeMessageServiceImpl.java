package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.basic.utils.FileUtils;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;
import hbnu.project.zhiyanbackend.knowledge.service.KnowledgeMessageService;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库消息服务实现
 * 负责知识库相关的消息通知发送
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeMessageServiceImpl implements KnowledgeMessageService {

    private final InboxMessageService inboxMessageService;
    private final ProjectMemberService projectMemberService;

    /**
     * 发送成果文件上传的通知
     * 发送给除了上传者的所有项目成员
     *
     * @param achievement 成果实体
     * @param file        文件实体
     * @param uploaderId  上传者ID
     */
    @Override
    public void notifyAchievementFileUpload(Achievement achievement, AchievementFile file, Long uploaderId) {
        if (achievement == null || file == null) {
            log.warn("文件上传通知参数不完整");
            return;
        }

        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(achievement.getProjectId());

            if (projectMemberIds.isEmpty()) {
                log.warn("项目[{}]没有成员，无法发送通知", achievement.getProjectId());
                return;
            }

            // 过滤上传者自己
            List<Long> filteredReceiverIds = projectMemberIds.stream()
                    .filter(receiverId -> !receiverId.equals(uploaderId))
                    .collect(Collectors.toList());

            if (filteredReceiverIds.isEmpty()) {
                log.info("过滤后没有接收者，跳过发送");
                return;
            }

            // 格式化文件大小
            String fileSizeStr = FileUtils.formatFileSize(file.getFileSize() != null ? file.getFileSize() : 0L);

            // 构建扩展数据JSON
            String extendDataJson = buildFileUploadExtendData(achievement, file);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILE_UPLOADED,
                    uploaderId,
                    filteredReceiverIds,
                    "成果文件上传",
                    String.format("成果「%s」有新文件上传\n文件名：「%s」\n文件大小：%s\n该成果已更新，请及时查看",
                            achievement.getTitle(), file.getFileName(), fileSizeStr),
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );

            log.info("文件上传通知发送成功: achievementId={}, fileId={}, receivers={}",
                    achievement.getId(), file.getId(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送文件上传通知失败: achievementId={}, fileId={}", achievement.getId(), file.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 发送成果文件删除的通知
     *
     * @param achievement 成果实体
     * @param file        文件实体
     * @param operatorId  操作者ID
     */
    @Override
    public void notifyAchievementFileDeleted(Achievement achievement, AchievementFile file, Long operatorId) {
        if (achievement == null || file == null) {
            log.warn("文件删除通知参数不完整");
            return;
        }

        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(achievement.getProjectId());

            if (projectMemberIds.isEmpty()) {
                log.warn("项目[{}]没有成员，无法发送通知", achievement.getProjectId());
                return;
            }

            // 过滤操作者自己
            List<Long> filteredReceiverIds = projectMemberIds.stream()
                    .filter(receiverId -> !receiverId.equals(operatorId))
                    .collect(Collectors.toList());

            if (filteredReceiverIds.isEmpty()) {
                log.info("过滤后没有接收者，跳过发送");
                return;
            }

            // 构建扩展数据JSON
            String extendDataJson = buildFileDeleteExtendData(achievement, file);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILE_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    "成果文件删除",
                    String.format("成果「%s」的文件「%s」已被删除，请及时查看",
                            achievement.getTitle(), file.getFileName()),
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );

            log.info("文件删除通知发送成功: achievementId={}, fileId={}, receivers={}",
                    achievement.getId(), file.getId(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送文件删除通知失败: achievementId={}, fileId={}", achievement.getId(), file.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 发送批量文件删除通知
     *
     * @param achievementMap 成果ID到成果实体的映射
     * @param files 被删除的文件列表
     * @param operatorId 操作者ID
     */
    @Override
    public void notifyAchievementFilesBatchDeleted(Map<Long, Achievement> achievementMap,
                                                   List<AchievementFile> files, Long operatorId) {
        if (achievementMap == null || files == null || files.isEmpty()) {
            log.warn("批量文件删除通知参数不完整");
            return;
        }

        try {
            // 按成果分组文件
            Map<Long, List<AchievementFile>> filesByAchievement = files.stream()
                    .collect(Collectors.groupingBy(AchievementFile::getAchievementId));

            // 为每个成果发送通知
            for (Map.Entry<Long, List<AchievementFile>> entry : filesByAchievement.entrySet()) {
                Long achievementId = entry.getKey();
                List<AchievementFile> achievementFiles = entry.getValue();
                Achievement achievement = achievementMap.get(achievementId);

                if (achievement != null && !achievementFiles.isEmpty()) {
                    sendBatchDeleteNotificationForAchievement(achievement, achievementFiles, operatorId);
                }
            }

            log.info("批量文件删除通知发送完成: totalFiles={}, affectedAchievements={}",
                    files.size(), filesByAchievement.size());
        } catch (Exception e) {
            log.error("发送批量文件删除通知失败", e);
            // 通知发送失败不影响主业务流程
        }
    }

    /**
     * 发送成果所有文件被删除的通知
     *
     * @param achievement 成果实体
     * @param files 被删除的文件列表
     * @param operatorId 操作者ID
     */
    @Override
    public void notifyAchievementAllFilesDeleted(Achievement achievement,
                                                 List<AchievementFile> files, Long operatorId) {
        if (achievement == null || files == null || files.isEmpty()) {
            log.warn("成果所有文件删除通知参数不完整");
            return;
        }

        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(achievement.getProjectId());

            if (projectMemberIds.isEmpty()) {
                log.warn("项目[{}]没有成员，无法发送通知", achievement.getProjectId());
                return;
            }

            // 过滤操作者自己
            List<Long> filteredReceiverIds = projectMemberIds.stream()
                    .filter(receiverId -> !receiverId.equals(operatorId))
                    .collect(Collectors.toList());

            if (filteredReceiverIds.isEmpty()) {
                log.info("过滤后没有接收者，跳过发送");
                return;
            }

            // 计算总文件大小
            long totalSize = files.stream()
                    .mapToLong(file -> file.getFileSize() != null ? file.getFileSize() : 0L)
                    .sum();

            // 构建扩展数据JSON
            String extendDataJson = buildAllFilesDeleteExtendData(achievement, files);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILES_BATCH_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    "成果文件批量删除",
                    String.format("成果「%s」的所有文件（共%d个，总大小%s）已被批量删除\n请及时查看成果状态",
                            achievement.getTitle(),
                            files.size(),
                            FileUtils.formatFileSize(totalSize)),
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );

            log.info("成果所有文件删除通知发送成功: achievementId={}, fileCount={}, receivers={}",
                    achievement.getId(), files.size(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送成果所有文件删除通知失败: achievementId={}", achievement.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 为单个成果发送批量删除通知
     */
    private void sendBatchDeleteNotificationForAchievement(Achievement achievement,
                                                           List<AchievementFile> files,
                                                           Long operatorId) {
        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(achievement.getProjectId());

            if (projectMemberIds.isEmpty()) {
                return;
            }

            // 过滤操作者自己
            List<Long> filteredReceiverIds = projectMemberIds.stream()
                    .filter(receiverId -> !receiverId.equals(operatorId))
                    .collect(Collectors.toList());

            if (filteredReceiverIds.isEmpty()) {
                return;
            }

            // 计算总文件大小
            long totalSize = files.stream()
                    .mapToLong(file -> file.getFileSize() != null ? file.getFileSize() : 0L)
                    .sum();

            // 构建文件名称列表（最多显示前3个）
            String fileNamesPreview = files.stream()
                    .map(AchievementFile::getFileName)
                    .limit(3)
                    .collect(Collectors.joining("、"));

            if (files.size() > 3) {
                fileNamesPreview += " 等";
            }

            // 构建扩展数据JSON
            String extendDataJson = buildBatchDeleteExtendData(achievement, files);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILES_BATCH_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    "成果文件批量删除",
                    String.format("成果「%s」的 %d 个文件已被批量删除\n涉及文件：%s\n总大小：%s\n请及时查看",
                            achievement.getTitle(),
                            files.size(),
                            fileNamesPreview,
                            FileUtils.formatFileSize(totalSize)),
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );

            log.debug("单个成果批量删除通知发送成功: achievementId={}, fileCount={}, receivers={}",
                    achievement.getId(), files.size(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送单个成果批量删除通知失败: achievementId={}", achievement.getId(), e);
            // 单个通知失败不影响其他通知
        }
    }

    /**
     * 构建文件上传扩展数据JSON
     */
    private String buildFileUploadExtendData(Achievement achievement, AchievementFile file) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileId", file.getId());
        extendData.put("fileName", file.getFileName());
        extendData.put("fileUrl", file.getMinioUrl());
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建文件删除扩展数据JSON
     */
    private String buildFileDeleteExtendData(Achievement achievement, AchievementFile file) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileId", file.getId());
        extendData.put("fileName", file.getFileName());
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建批量删除扩展数据JSON
     */
    private String buildBatchDeleteExtendData(Achievement achievement, List<AchievementFile> files) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileCount", files.size());
        extendData.put("fileNames", files.stream()
                .map(AchievementFile::getFileName)
                .collect(Collectors.toList()));
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建所有文件删除扩展数据JSON
     */
    private String buildAllFilesDeleteExtendData(Achievement achievement, List<AchievementFile> files) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileCount", files.size());
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }
}
