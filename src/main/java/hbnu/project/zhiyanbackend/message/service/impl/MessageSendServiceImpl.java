package hbnu.project.zhiyanbackend.message.service.impl;

import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.FileUtils;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;
import hbnu.project.zhiyanbackend.message.service.MessageSendService;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskSubmission;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 * 负责项目所有业务相关的消息通知发送
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSendServiceImpl implements MessageSendService {

    private final InboxMessageService inboxMessageService;
    private final ProjectMemberService projectMemberService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    /**
     * 发送批量文件上传通知
     * 发送给除了上传者的所有项目成员
     *
     * @param achievement 成果实体
     * @param files       上传的文件列表
     * @param uploaderId  上传者ID
     */
    @Override
    public void notifyAchievementFilesBatchUpload(Achievement achievement, List<AchievementFile> files, Long uploaderId) {
        if (achievement == null || files == null || files.isEmpty()) {
            log.warn("批量文件上传通知参数不完整");
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

            // 获取上传者姓名
            String uploaderName = getOperatorName(uploaderId);

            // 构建扩展数据JSON
            String extendDataJson = buildBatchUploadExtendData(achievement, files, uploaderId, uploaderName);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILES_BATCH_UPLOADED,
                    uploaderId,
                    filteredReceiverIds,
                    "成果文件批量上传",
                    String.format("成果「%s」有 %d 个文件被批量上传\n涉及文件：%s\n总大小：%s\n上传者：%s\n该成果已更新，请及时查看",
                            achievement.getTitle(),
                            files.size(),
                            fileNamesPreview,
                            FileUtils.formatFileSize(totalSize),
                            uploaderName),
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );

            log.info("批量文件上传通知发送成功: achievementId={}, fileCount={}, receivers={}",
                    achievement.getId(), files.size(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送批量文件上传通知失败: achievementId={}", achievement.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

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

            // 获取上传者姓名
            String uploaderName = getOperatorName(uploaderId);

            // 构建扩展数据JSON
            String extendDataJson = buildFileUploadExtendData(achievement, file, uploaderId, uploaderName);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILE_UPLOADED,
                    uploaderId,
                    filteredReceiverIds,
                    "成果文件上传",
                    String.format("成果「%s」有新文件上传\n文件名：「%s」\n文件大小：%s\n上传者：%s\n该成果已更新，请及时查看",
                            achievement.getTitle(), file.getFileName(), fileSizeStr, uploaderName),
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

            // 获取操作者姓名
            String operatorName = getOperatorName(operatorId);

            // 构建扩展数据JSON
            String extendDataJson = buildFileDeleteExtendData(achievement, file, operatorId, operatorName);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILE_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    "成果文件删除",
                    String.format("成果「%s」的文件「%s」已被删除\n操作者：%s\n请及时查看",
                            achievement.getTitle(), file.getFileName(), operatorName),
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

            // 获取操作者姓名
            String operatorName = getOperatorName(operatorId);

            // 构建扩展数据JSON
            String extendDataJson = buildAllFilesDeleteExtendData(achievement, files, operatorId, operatorName);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILES_BATCH_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    "成果文件批量删除",
                    String.format("成果「%s」的所有文件（共%d个，总大小%s）已被批量删除\n操作者：%s\n请及时查看成果状态",
                            achievement.getTitle(),
                            files.size(),
                            FileUtils.formatFileSize(totalSize),
                            operatorName),
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
     * 发送 更新成果状态 通知给项目成员
     *
     * @param achievement 更改的成果
     * @param oldStatus   旧状态
     * @param status      新状态
     * @param operatorId  操作的用户id
     */
    @Override
    public void notifyAchievementStatusChange(Achievement achievement, AchievementStatus oldStatus, AchievementStatus status, Long operatorId) {
        if(achievement == null || oldStatus == null || status == null){
            log.warn("啥都没有就发送？？");
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

            // 获取操作者姓名
            String operatorName = getOperatorName(operatorId);

            // 获取状态显示名称
            String oldStatusName = getStatusDisplayName(oldStatus);
            String newStatusName = getStatusDisplayName(status);

            // 构建扩展数据JSON
            String extendDataJson = buildStatusChangeExtendData(achievement, oldStatus, status, operatorId, operatorName);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_STATUS_CHANGED,
                    operatorId,
                    filteredReceiverIds,
                    "成果状态更新",
                    String.format("成果「%s」的状态已更新\n原状态：%s\n新状态：%s\n操作者：%s\n请及时查看",
                            achievement.getTitle(), oldStatusName, newStatusName, operatorName),
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );

            log.info("成果状态更新通知发送成功: achievementId={}, oldStatus={}, newStatus={}, receivers={}",
                    achievement.getId(), oldStatus, status, filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送更新成果状态通知失败: achievementId={}", achievement.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 发送 成果创建的通知
     *
     * @param achievement 创建的成果
     */
    @Override
    public void notifyAchievementCreated(Achievement achievement) {
        if (achievement == null) {
            log.warn("成果创建通知参数不完整");
            return;
        }

        List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(achievement.getProjectId());

        Long operatorId = achievement.getCreatorId();
        Long projectId = achievement.getProjectId();
        String projectName = projectRepository.findProjectNameById(projectId).orElse("未知项目");

        if (projectMemberIds.isEmpty()) {
            log.warn("项目[{}]没有成员，无法发送通知", projectName);
            return;
        }

        // 过滤掉创建者自己
        List<Long> filteredReceiverIds = projectMemberIds.stream()
                .filter(memberId -> !memberId.equals(operatorId))
                .toList();

        if (filteredReceiverIds.isEmpty()) {
            log.info("过滤后没有接收者，跳过发送");
            return;
        }

        try{
            // 获取创建者姓名
            String creatorName = getOperatorName(operatorId);
            // 构建扩展数据JSON
            String extendDataJson = buildAchievementCreatedExtendData(achievement, projectName, creatorName);

            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_CREATED,
                    operatorId,
                    filteredReceiverIds,
                    "新成果创建",
                    String.format("项目「%s」中有新成果「%s」创建\n成果类型：%s\n创建者：「%s」\n请及时查看",
                            projectName,
                            achievement.getTitle(),
                            achievement.getType() != null ? achievement.getType().getName() : "未知类型",
                            creatorName),
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );
            log.info("成果创建通知发送成功: achievementId={}, projectName={}, receivers={}",
                    achievement.getId(), projectName, filteredReceiverIds.size());
        }catch (ServiceException e){
            log.error("发送成果创建通知失败 - ServiceException: achievementId={}", achievement.getId(), e);
            // 服务异常，可能是业务逻辑错误
        } catch (Exception e) {
            log.error("发送成果创建通知失败: achievementId={}", achievement.getId(), e);
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

            // 获取操作者姓名
            String operatorName = getOperatorName(operatorId);

            // 构建扩展数据JSON
            String extendDataJson = buildBatchDeleteExtendData(achievement, files, operatorId, operatorName);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_FILES_BATCH_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    "成果文件批量删除",
                    String.format("成果「%s」的 %d 个文件已被批量删除\n涉及文件：%s\n总大小：%s\n操作者：%s\n请及时查看",
                            achievement.getTitle(),
                            files.size(),
                            fileNamesPreview,
                            FileUtils.formatFileSize(totalSize),
                            operatorName),
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
     * 发送任务提交审核结果通知
     * 发送给任务提交者
     *
     * @param task 任务实体
     * @param submission 任务提交记录
     * @param reviewStatus 审核状态（APPROVED或REJECTED）
     * @param reviewerId 审核人ID
     */
    @Override
    public void notifyTaskSubmissionReviewed(Task task, TaskSubmission submission, ReviewStatus reviewStatus, Long reviewerId) {
        if (task == null || submission == null || reviewStatus == null) {
            log.warn("任务审核结果通知参数不完整");
            return;
        }

        // 只处理审核通过和审核拒绝的情况
        if (reviewStatus != ReviewStatus.APPROVED && reviewStatus != ReviewStatus.REJECTED) {
            log.warn("任务审核结果通知只支持APPROVED和REJECTED状态，当前状态: {}", reviewStatus);
            return;
        }

        try {
            // 获取审核人姓名
            String reviewerName = getOperatorName(reviewerId);

            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(task.getProjectId())
                    .orElse("未知项目");

            // 获取提交人ID（接收者）
            Long submitterId = submission.getSubmitterId();

            // 根据审核状态构建不同的消息内容
            String title;
            String content;

            if (reviewStatus == ReviewStatus.APPROVED) {
                title = "任务审核通过";
                content = String.format("您提交的任务「%s」已通过审核\n项目：%s\n审核人：%s\n审核意见：%s\n请查看任务详情",
                        task.getTitle(),
                        projectName,
                        reviewerName,
                        submission.getReviewComment() != null ? submission.getReviewComment() : "无");
            } else { // REJECTED
                title = "任务审核被退回";
                content = String.format("您提交的任务「%s」审核未通过，已被退回\n项目：%s\n审核人：%s\n退回原因：%s\n请根据审核意见修改后重新提交",
                        task.getTitle(),
                        projectName,
                        reviewerName,
                        submission.getReviewComment() != null ? submission.getReviewComment() : "无");
            }

            // 构建扩展数据JSON
            String extendDataJson = buildTaskSubmissionReviewedExtendData(task, submission, reviewStatus, reviewerId, reviewerName, projectName);

            // 发送消息
            // 注意：InboxMessageService的sendPersonalMessage会自动根据scene设置priority
            // TASK_REVIEW_RESULT场景对应HIGH优先级，满足审核拒绝需要最高优先级的要求
            inboxMessageService.sendPersonalMessage(
                    MessageScene.TASK_REVIEW_RESULT,
                    reviewerId,
                    submitterId,
                    title,
                    content,
                    task.getId(),
                    "TASK",
                    extendDataJson
            );

            log.info("任务审核结果通知发送成功: taskId={}, submissionId={}, reviewStatus={}, submitterId={}",
                    task.getId(), submission.getId(), reviewStatus, submitterId);
        } catch (Exception e) {
            log.error("发送任务审核结果通知失败: taskId={}, submissionId={}", task.getId(), submission.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 构建文件上传扩展数据JSON
     */
    private String buildFileUploadExtendData(Achievement achievement, AchievementFile file, Long uploaderId, String uploaderName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileId", file.getId());
        extendData.put("fileName", file.getFileName());
        extendData.put("fileUrl", file.getCosUrl());
        extendData.put("uploaderId", uploaderId);
        extendData.put("uploaderName", uploaderName);
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建文件删除扩展数据JSON
     */
    private String buildFileDeleteExtendData(Achievement achievement, AchievementFile file, Long operatorId, String operatorName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileId", file.getId());
        extendData.put("fileName", file.getFileName());
        extendData.put("operatorId", operatorId);
        extendData.put("operatorName", operatorName);
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建批量删除扩展数据JSON
     */
    private String buildBatchDeleteExtendData(Achievement achievement, List<AchievementFile> files, Long operatorId, String operatorName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileCount", files.size());
        extendData.put("fileNames", files.stream()
                .map(AchievementFile::getFileName)
                .collect(Collectors.toList()));
        extendData.put("operatorId", operatorId);
        extendData.put("operatorName", operatorName);
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建所有文件删除扩展数据JSON
     */
    private String buildAllFilesDeleteExtendData(Achievement achievement, List<AchievementFile> files, Long operatorId, String operatorName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileCount", files.size());
        extendData.put("operatorId", operatorId);
        extendData.put("operatorName", operatorName);
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建状态变更扩展数据JSON
     */
    private String buildStatusChangeExtendData(Achievement achievement, AchievementStatus oldStatus, 
                                               AchievementStatus newStatus, Long operatorId, String operatorName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("oldStatus", oldStatus.name());
        extendData.put("newStatus", newStatus.name());
        extendData.put("oldStatusName", getStatusDisplayName(oldStatus));
        extendData.put("newStatusName", getStatusDisplayName(newStatus));
        extendData.put("operatorId", operatorId);
        extendData.put("operatorName", operatorName);
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建批量上传扩展数据JSON
     */
    private String buildBatchUploadExtendData(Achievement achievement, List<AchievementFile> files, Long uploaderId, String uploaderName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("fileCount", files.size());
        extendData.put("fileNames", files.stream()
                .map(AchievementFile::getFileName)
                .collect(Collectors.toList()));
        extendData.put("fileIds", files.stream()
                .map(AchievementFile::getId)
                .collect(Collectors.toList()));
        extendData.put("uploaderId", uploaderId);
        extendData.put("uploaderName", uploaderName);
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建成果创建扩展数据JSON
     */
    private String buildAchievementCreatedExtendData(Achievement achievement, String projectName, String creatorName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("projectId", achievement.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("creatorId", achievement.getCreatorId());
        extendData.put("creatorName", creatorName);
        extendData.put("achievementType", achievement.getType() != null ? achievement.getType().getName() : "未知类型");
        extendData.put("createdAt", achievement.getCreatedAt());
        extendData.put("redirectUrl", "/knowledge/achievement/" + achievement.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建任务审核结果扩展数据JSON
     */
    private String buildTaskSubmissionReviewedExtendData(Task task, TaskSubmission submission,
                                                         ReviewStatus reviewStatus, Long reviewerId,
                                                         String reviewerName, String projectName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("taskId", task.getId());
        extendData.put("taskTitle", task.getTitle());
        extendData.put("submissionId", submission.getId());
        extendData.put("submissionVersion", submission.getVersion());
        extendData.put("projectId", task.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("reviewStatus", reviewStatus.name());
        extendData.put("reviewStatusName", reviewStatus.getDescription());
        extendData.put("reviewerId", reviewerId);
        extendData.put("reviewerName", reviewerName);
        extendData.put("reviewComment", submission.getReviewComment());
        extendData.put("submitterId", submission.getSubmitterId());
        extendData.put("redirectUrl", "/tasks/" + task.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 获取操作者姓名
     * 如果查询失败或用户不存在，返回"未知用户"
     *
     * @param operatorId 操作者ID
     * @return 操作者姓名
     */
    private String getOperatorName(Long operatorId) {
        if (operatorId == null) {
            return "未知用户";
        }
        try {
            return userRepository.findNameById(operatorId)
                    .orElse("未知用户");
        } catch (Exception e) {
            log.warn("获取操作者姓名失败: operatorId={}", operatorId, e);
            return "未知用户";
        }
    }

    /**
     * 获取状态显示名称
     *
     * @param status 成果状态
     * @return 状态显示名称
     */
    private String getStatusDisplayName(AchievementStatus status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case draft:
                return "草稿";
            case under_review:
                return "审核中";
            case published:
                return "已发布";
            case obsolete:
                return "已过时";
            default:
                return status.name();
        }
    }
}
