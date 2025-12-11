package hbnu.project.zhiyanbackend.message.service.impl;

import cn.hutool.core.util.StrUtil;
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
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage;
import hbnu.project.zhiyanbackend.wiki.model.enums.PageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
     * 发送任务提醒通知
     * 从任务截止的那天那小时开始算，每截止前72小时，48小时，24小时发送一次任务提醒通知
     *
     * @param task 消息针对的任务
     * @param receiverId 接收人
     */
    @Override
    public void notifyTaskNeedSubmission(Task task, Long receiverId) {
        if (task == null || receiverId == null || task.getDueDate() == null) {
            log.warn("任务提醒通知参数不完整: task={}, receiverId={}", task, receiverId);
            return;
        }

        try{
            // 获取接收者姓名
            String receiverName = getOperatorName(receiverId);
            // 获取任务创建者姓名
            String creatorName = getOperatorName(task.getCreatorId());
            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(task.getProjectId())
                    .orElse("未知项目");

            // 计算距离截止日期的小时数
            LocalDate today = LocalDate.now();
            long daysUntilDue = ChronoUnit.DAYS.between(today, task.getDueDate());
            long hoursUntilDue = daysUntilDue * 24;

            // 构建提醒时间描述
            String timeDesc;
            if (hoursUntilDue <= 24) {
                timeDesc = "24小时内";
            } else if (hoursUntilDue <= 48) {
                timeDesc = "48小时内";
            } else if (hoursUntilDue <= 72) {
                timeDesc = "72小时内";
            } else {
                log.info("任务[{}]距离截止日期超过72小时，不发送提醒", task.getId());
                return;
            }

            // 构建消息内容
            String title = "任务提交提醒";
            String content = String.format("您有任务即将截止，请及时提交\n任务名称：「%s」\n项目：%s\n截止日期：%s\n优先级：%s\n创建者：%s\n距离截止还有%s，请尽快完成并提交",
                    task.getTitle(),
                    projectName,
                    task.getDueDate().toString(),
                    task.getPriority().name(),
                    creatorName,
                    timeDesc);

            // 构建扩展数据JSON
            String extendDataJson = buildTaskReminderExtendData(task, receiverId, receiverName, projectName, timeDesc);

            // 发送消息
            inboxMessageService.sendPersonalMessage(
                    MessageScene.TASK_DEADLINE_REMIND,
                    task.getCreatorId(),
                    receiverId,
                    title,
                    content,
                    task.getId(),
                    "TASK",
                    extendDataJson
            );

            log.info("任务提醒通知发送成功: taskId={}, receiverId={}, hoursUntilDue={}",
                    task.getId(), receiverId, hoursUntilDue);
        }catch (ServiceException e){
            log.error("发送任务提醒通知失败: taskId={}, receiverId={}", task.getId(), receiverId, e);
        }
    }

    /**
     * 发送任务逾期通知
     * 任务截止的那天开始，如果任务没有提交，发送一条任务逾期警告
     *
     * @param task 消息针对的任务
     * @param receiverId 接收人
     */
    @Override
    public void notifyTaskOverSubmissionTime(Task task, Long receiverId, long overdueDays) {
        if (task == null || receiverId == null || task.getDueDate() == null) {
            log.warn("任务逾期通知参数不完整: task={}, receiverId={}", task, receiverId);
            return;
        }

        // 检查是否确实逾期
        if (LocalDate.now().isBefore(task.getDueDate())) {
            log.info("任务[{}]尚未逾期，不发送逾期通知", task.getId());
            return;
        }

        try {
            // 获取接收者姓名
            String receiverName = getOperatorName(receiverId);
            // 获取任务创建者姓名
            String creatorName = getOperatorName(task.getCreatorId());
            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(task.getProjectId())
                    .orElse("未知项目");

            // 构建消息内容
            String title = "任务逾期警告";
            String content = String.format("您有任务已逾期，请尽快处理\n任务名称：「%s」\n项目：%s\n原定截止日期：%s\n优先级：%s\n创建者：%s\n，任务已经逾期%s天，逾期三天后该消息不会再发送，请尽快完成并提交",
                    task.getTitle(),
                    projectName,
                    task.getDueDate().toString(),
                    task.getPriority().name(),
                    creatorName,
                    overdueDays);

            // 构建扩展数据JSON
            String extendDataJson = buildTaskOverdueExtendData(task, receiverId, receiverName, projectName);

            // 发送消息（高优先级）
            inboxMessageService.sendPersonalMessage(
                    MessageScene.TASK_OVERDUE,
                    task.getCreatorId(),
                    receiverId,
                    title,
                    content,
                    task.getId(),
                    "TASK",
                    extendDataJson
            );

            log.info("任务逾期通知发送成功: taskId={}, receiverId={}", task.getId(), receiverId);
        }catch (ServiceException e){
            log.error("发送任务逾期通知失败: taskId={}, receiverId={}", task.getId(), receiverId, e);
        }
    }

    /**
     * 发送待审核任务通知
     * 当任务被提交后，通知任务创建者（审核者）有新的待审核任务
     *
     * @param task 任务实体
     * @param submission 任务提交记录
     * @param submitterId 提交者ID
     */
    @Override
    public void notifyTaskReviewRequest(Task task, TaskSubmission submission, Long submitterId) {
        if (task == null || submission == null || submitterId == null) {
            log.warn("待审核任务通知参数不完整: task={}, submission={}, submitterId={}", task, submission, submitterId);
            return;
        }

        // 获取任务创建者ID（审核者）
        Long reviewerId = task.getCreatorId();
        if (reviewerId == null) {
            log.warn("任务[{}]没有创建者，无法发送待审核通知", task.getId());
            return;
        }

        // 如果提交者就是创建者，不需要通知自己
        if (reviewerId.equals(submitterId)) {
            log.info("任务[{}]的提交者是创建者，无需发送待审核通知", task.getId());
            return;
        }

        try {
            // 获取提交者姓名
            String submitterName = getOperatorName(submitterId);
            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(task.getProjectId()).orElse("未知项目");

            // 构建消息内容
            String title = "待审核任务提醒";
            String content = String.format("您创建的任务「%s」有新提交，等待您的审核\n项目：%s\n提交者：%s\n提交版本：v%d\n提交内容：%s\n请及时审核",
                    task.getTitle(),
                    projectName,
                    submitterName,
                    submission.getVersion(),
                    submission.getSubmissionContent() != null && submission.getSubmissionContent().length() > 100
                            ? submission.getSubmissionContent().substring(0, 100) + "..."
                            : (submission.getSubmissionContent() != null ? submission.getSubmissionContent() : "无"));

            // 构建扩展数据JSON
            String extendDataJson = buildTaskReviewRequestExtendData(task, submission, submitterId, submitterName, projectName);
            // 发送消息给任务创建者（审核者）
            inboxMessageService.sendPersonalMessage(
                    MessageScene.TASK_REVIEW_REQUEST,
                    submitterId,
                    reviewerId,
                    title,
                    content,
                    task.getId(),
                    "TASK",
                    extendDataJson
            );
            log.info("待审核任务通知发送成功: taskId={}, submissionId={}, reviewerId={}", task.getId(), submission.getId(), reviewerId);
        } catch (Exception e) {
            log.error("发送待审核任务通知失败: taskId={}, submissionId={}", task.getId(), submission.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 发送成果删除通知
     * 发送给除了删除者的所有项目成员
     *
     * @param achievement 被删除的成果实体
     * @param operatorId 操作者ID
     */
    @Override
    public void notifyAchievementDeleted(Achievement achievement, Long operatorId) {
        if (achievement == null || operatorId == null) {
            log.warn("成果删除通知参数不完整: achievement={}, operatorId={}", achievement, operatorId);
            return;
        }

        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(achievement.getProjectId());

            if (projectMemberIds.isEmpty()) {
                log.warn("项目[{}]没有成员，无法发送通知", achievement.getProjectId());
                return;
            }

            // 过滤删除者自己
            List<Long> filteredReceiverIds = projectMemberIds.stream()
                    .filter(receiverId -> !receiverId.equals(operatorId))
                    .collect(Collectors.toList());

            if (filteredReceiverIds.isEmpty()) {
                log.info("过滤后没有接收者，跳过发送");
                return;
            }

            // 获取操作者姓名
            String operatorName = getOperatorName(operatorId);
            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(achievement.getProjectId())
                    .orElse("未知项目");

            // 构建扩展数据JSON
            String extendDataJson = buildAchievementDeletedExtendData(achievement, operatorId, operatorName, projectName);

            // 构建消息内容
            String title = "成果删除通知";
            String content = String.format("项目「%s」中的成果「%s」已被删除\n操作者：%s\n成果类型：%s\n请及时查看项目状态",
                    projectName,
                    achievement.getTitle(),
                    operatorName,
                    achievement.getType() != null ? achievement.getType().getName() : "未知类型");

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.ACHIEVEMENT_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    title,
                    content,
                    achievement.getId(),
                    "ACHIEVEMENT",
                    extendDataJson
            );

            log.info("成果删除通知发送成功: achievementId={}, receivers={}",
                    achievement.getId(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送成果删除通知失败: achievementId={}", achievement.getId(), e);
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
     * 发送账号异地登录安全通知
     * 发送给用户自己
     *
     * @param userId 用户ID
     * @param currentIp 当前登录IP
     * @param currentLocation 当前登录位置
     * @param lastIp 上次登录IP（可能为空）
     * @param lastLocation 上次登录位置（可能为空）
     */
    @Override
    public void notifyAccountSecurityAlert(Long userId, String currentIp, String currentLocation,
                                           String lastIp, String lastLocation) {
        if (userId == null || currentIp == null) {
            log.warn("账号安全通知参数不完整");
            return;
        }

        try {
            // 获取用户姓名
            String userName = userRepository.findNameById(userId)
                    .orElse("用户");

            // 构建消息内容
            String title = "账号安全提醒";
            StringBuilder content = new StringBuilder();
            content.append("您的账号在异地登录\n");
            content.append("当前登录IP：").append(currentIp).append("\n");
            content.append("当前登录位置：").append(currentLocation).append("\n");

            if (StrUtil.isNotBlank(lastIp)) {
                content.append("上次登录IP：").append(lastIp).append("\n");
            }
            if (StrUtil.isNotBlank(lastLocation)) {
                content.append("上次登录位置：").append(lastLocation).append("\n");
            }

            content.append("\n如果这不是您的操作且您为授权给他人，几乎可以确定你的账号被盗用，请立即修改密码并联系管理员。");

            // 构建扩展数据
            Map<String, Object> extendData = new HashMap<>();
            extendData.put("userId", userId);
            extendData.put("userName", userName);
            extendData.put("currentIp", currentIp);
            extendData.put("currentLocation", currentLocation);
            extendData.put("lastIp", lastIp);
            extendData.put("lastLocation", lastLocation);
            extendData.put("redirectUrl", "/user/security");
            String extendDataJson = JsonUtils.toJsonString(extendData);

            // 发送消息给自己（senderId为null表示系统消息）
            inboxMessageService.sendPersonalMessage(
                    MessageScene.SYSTEM_SECURITY_ALERT,
                    null, // 系统消息，发送者为null
                    userId,
                    title,
                    content.toString(),
                    userId,
                    "USER",
                    extendDataJson
            );

            log.info("账号安全通知发送成功: userId={}, currentIp={}, currentLocation={}",
                    userId, currentIp, currentLocation);
        } catch (Exception e) {
            log.error("发送账号安全通知失败: userId={}, currentIp={}", userId, currentIp, e);
            // 通知发送失败不影响登录流程
        }
    }

    /**
     * 发送Wiki页面创建通知
     * 发送给除了创建者的所有项目成员
     *
     * @param wikiPage Wiki页面实体
     * @param creatorId 创建者ID
     */
    @Override
    public void notifyWikiPageCreated(WikiPage wikiPage, Long creatorId) {
        if (wikiPage == null) {
            log.warn("Wiki页面创建通知参数不完整");
            return;
        }

        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(wikiPage.getProjectId());

            if (projectMemberIds.isEmpty()) {
                log.warn("项目[{}]没有成员，无法发送通知", wikiPage.getProjectId());
                return;
            }

            // 过滤创建者自己
            List<Long> filteredReceiverIds = projectMemberIds.stream()
                    .filter(receiverId -> !receiverId.equals(creatorId))
                    .collect(Collectors.toList());

            if (filteredReceiverIds.isEmpty()) {
                log.info("过滤后没有接收者，跳过发送");
                return;
            }

            // 获取创建者姓名
            String creatorName = getOperatorName(creatorId);

            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(wikiPage.getProjectId())
                    .orElse("未知项目");

            // 构建扩展数据JSON
            String extendDataJson = buildWikiPageCreatedExtendData(wikiPage, creatorId, creatorName, projectName);

            // 构建消息内容
            String pageTypeName = wikiPage.getPageType() == PageType.DIRECTORY ? "目录" : "文档";
            String title = "Wiki页面创建";
            String content = String.format("项目「%s」中创建了新的Wiki%s「%s」\n创建者：%s\n路径：%s\n请及时查看",
                    projectName,
                    pageTypeName,
                    wikiPage.getTitle(),
                    creatorName,
                    wikiPage.getPath());

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.WIKI_PAGE_CREATED,
                    creatorId,
                    filteredReceiverIds,
                    title,
                    content,
                    wikiPage.getId(),
                    "WIKI",
                    extendDataJson
            );

            log.info("Wiki页面创建通知发送成功: wikiPageId={}, pageType={}, receivers={}",
                    wikiPage.getId(), wikiPage.getPageType(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送Wiki页面创建通知失败: wikiPageId={}", wikiPage.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 发送Wiki页面更新通知
     * 发送给除了编辑者的所有项目成员
     *
     * @param wikiPage Wiki页面实体
     * @param editorId 编辑者ID
     * @param changeDesc 修改说明
     */
    @Override
    public void notifyWikiPageUpdated(WikiPage wikiPage, Long editorId, String changeDesc) {
        if (wikiPage == null) {
            log.warn("Wiki页面更新通知参数不完整");
            return;
        }

        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(wikiPage.getProjectId());

            if (projectMemberIds.isEmpty()) {
                log.warn("项目[{}]没有成员，无法发送通知", wikiPage.getProjectId());
                return;
            }

            // 过滤编辑者自己
            List<Long> filteredReceiverIds = projectMemberIds.stream()
                    .filter(receiverId -> !receiverId.equals(editorId))
                    .collect(Collectors.toList());

            if (filteredReceiverIds.isEmpty()) {
                log.info("过滤后没有接收者，跳过发送");
                return;
            }

            // 获取编辑者姓名
            String editorName = getOperatorName(editorId);

            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(wikiPage.getProjectId())
                    .orElse("未知项目");

            // 构建扩展数据JSON
            String extendDataJson = buildWikiPageUpdatedExtendData(wikiPage, editorId, editorName, projectName, changeDesc);

            // 构建消息内容
            String pageTypeName = wikiPage.getPageType() == PageType.DIRECTORY ? "目录" : "文档";
            String title = "Wiki页面更新";
            String content = String.format("项目「%s」中的Wiki%s「%s」已更新\n编辑者：%s\n修改说明：%s\n路径：%s\n请及时查看",
                    projectName,
                    pageTypeName,
                    wikiPage.getTitle(),
                    editorName,
                    StringUtils.hasText(changeDesc) ? changeDesc : "无",
                    wikiPage.getPath());

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.WIKI_PAGE_UPDATED,
                    editorId,
                    filteredReceiverIds,
                    title,
                    content,
                    wikiPage.getId(),
                    "WIKI",
                    extendDataJson
            );

            log.info("Wiki页面更新通知发送成功: wikiPageId={}, pageType={}, receivers={}",
                    wikiPage.getId(), wikiPage.getPageType(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送Wiki页面更新通知失败: wikiPageId={}", wikiPage.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 发送Wiki页面删除通知
     * 发送给除了删除者的所有项目成员
     *
     * @param wikiPage Wiki页面实体
     * @param operatorId 操作者ID
     */
    @Override
    public void notifyWikiPageDeleted(WikiPage wikiPage, Long operatorId) {
        if (wikiPage == null) {
            log.warn("Wiki页面删除通知参数不完整");
            return;
        }

        try {
            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(wikiPage.getProjectId());

            if (projectMemberIds.isEmpty()) {
                log.warn("项目[{}]没有成员，无法发送通知", wikiPage.getProjectId());
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

            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(wikiPage.getProjectId())
                    .orElse("未知项目");

            // 构建扩展数据JSON
            String extendDataJson = buildWikiPageDeletedExtendData(wikiPage, operatorId, operatorName, projectName);

            // 构建消息内容
            String pageTypeName = wikiPage.getPageType() == PageType.DIRECTORY ? "目录" : "文档";
            String title = "Wiki页面删除";
            String content = String.format("项目「%s」中的Wiki%s「%s」已被删除\n操作者：%s\n路径：%s\n请及时查看",
                    projectName,
                    pageTypeName,
                    wikiPage.getTitle(),
                    operatorName,
                    wikiPage.getPath());

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.WIKI_PAGE_DELETED,
                    operatorId,
                    filteredReceiverIds,
                    title,
                    content,
                    wikiPage.getId(),
                    "WIKI",
                    extendDataJson
            );

            log.info("Wiki页面删除通知发送成功: wikiPageId={}, pageType={}, receivers={}",
                    wikiPage.getId(), wikiPage.getPageType(), filteredReceiverIds.size());
        } catch (Exception e) {
            log.error("发送Wiki页面删除通知失败: wikiPageId={}", wikiPage.getId(), e);
            // 通知发送失败不影响主流程
        }
    }

    /**
     * 发送邮箱修改通知
     * 发送给用户自己（高优先级系统消息）
     *
     * @param userId 用户ID
     * @param oldEmail 旧邮箱
     * @param newEmail 新邮箱
     */
    @Override
    public void notifyEmailChanged(Long userId, String oldEmail, String newEmail) {
        if (userId == null || newEmail == null) {
            log.warn("邮箱修改通知参数不完整");
            return;
        }

        try {
            // 获取用户姓名
            String userName = userRepository.findNameById(userId)
                    .orElse("用户");

            // 构建消息内容
            String title = "邮箱修改成功";
            StringBuilder content = new StringBuilder();
            content.append("您的账号邮箱已成功修改\n");
            if (oldEmail != null) {
                content.append("原邮箱：").append(oldEmail).append("\n");
            }
            content.append("新邮箱：").append(newEmail).append("\n");
            content.append("\n");
            content.append("重要提示：\n");
            content.append("1. 您的账号已自动登出，请使用新邮箱重新登录\n");
            content.append("2. 请妥善保管新邮箱，用于后续登录和找回密码\n");
            content.append("3. 如非本人操作，请立即联系管理员冻结账号并修改密码");

            // 构建扩展数据
            Map<String, Object> extendData = new HashMap<>();
            extendData.put("userId", userId);
            extendData.put("userName", userName);
            extendData.put("oldEmail", oldEmail);
            extendData.put("newEmail", newEmail);
            extendData.put("redirectUrl", "/user/profile");
            String extendDataJson = JsonUtils.toJsonString(extendData);

            // 发送高优先级系统消息（senderId为null表示系统消息）
            inboxMessageService.sendPersonalMessage(
                    MessageScene.USER_EMAIL_CHANGED,
                    null,
                    userId,
                    title,
                    content.toString(),
                    userId,
                    "USER",
                    extendDataJson
            );

            log.info("邮箱修改通知发送成功: userId={}, oldEmail={}, newEmail={}",
                    userId, oldEmail, newEmail);
        } catch (Exception e) {
            log.error("发送邮箱修改通知失败: userId={}, newEmail={}", userId, newEmail, e);
            // 通知发送失败不影响主流程
        }
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
     * 构建Wiki页面创建扩展数据JSON
     */
    private String buildWikiPageCreatedExtendData(WikiPage wikiPage, Long creatorId, String creatorName, String projectName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("wikiPageId", wikiPage.getId());
        extendData.put("wikiPageTitle", wikiPage.getTitle());
        extendData.put("pageType", wikiPage.getPageType().name());
        extendData.put("pageTypeName", wikiPage.getPageType() == PageType.DIRECTORY ? "目录" : "文档");
        extendData.put("path", wikiPage.getPath());
        extendData.put("projectId", wikiPage.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("creatorId", creatorId);
        extendData.put("creatorName", creatorName);
        extendData.put("redirectUrl", "/wiki/page/" + wikiPage.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建待审核任务扩展数据JSON
     */
    private String buildTaskReviewRequestExtendData(Task task, TaskSubmission submission,
                                                    Long submitterId, String submitterName, String projectName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("taskId", task.getId());
        extendData.put("taskTitle", task.getTitle());
        extendData.put("submissionId", submission.getId());
        extendData.put("submissionVersion", submission.getVersion());
        extendData.put("projectId", task.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("submitterId", submitterId);
        extendData.put("submitterName", submitterName);
        extendData.put("submissionContent", submission.getSubmissionContent());
        extendData.put("dueDate", task.getDueDate());
        extendData.put("priority", task.getPriority().name());
        extendData.put("redirectUrl", "/tasks/" + task.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建成果删除扩展数据JSON
     */
    private String buildAchievementDeletedExtendData(Achievement achievement, Long operatorId,
                                                     String operatorName, String projectName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("achievementId", achievement.getId());
        extendData.put("achievementTitle", achievement.getTitle());
        extendData.put("projectId", achievement.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("operatorId", operatorId);
        extendData.put("operatorName", operatorName);
        extendData.put("achievementType", achievement.getType() != null ? achievement.getType().getName() : "未知类型");
        extendData.put("redirectUrl", "/knowledge/project/" + achievement.getProjectId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建Wiki页面更新扩展数据JSON
     */
    private String buildWikiPageUpdatedExtendData(WikiPage wikiPage, Long editorId, String editorName, String projectName, String changeDesc) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("wikiPageId", wikiPage.getId());
        extendData.put("wikiPageTitle", wikiPage.getTitle());
        extendData.put("pageType", wikiPage.getPageType().name());
        extendData.put("pageTypeName", wikiPage.getPageType() == PageType.DIRECTORY ? "目录" : "文档");
        extendData.put("path", wikiPage.getPath());
        extendData.put("projectId", wikiPage.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("editorId", editorId);
        extendData.put("editorName", editorName);
        extendData.put("changeDescription", changeDesc);
        extendData.put("currentVersion", wikiPage.getCurrentVersion());
        extendData.put("redirectUrl", "/wiki/page/" + wikiPage.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建Wiki页面删除扩展数据JSON
     */
    private String buildWikiPageDeletedExtendData(WikiPage wikiPage, Long operatorId, String operatorName, String projectName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("wikiPageId", wikiPage.getId());
        extendData.put("wikiPageTitle", wikiPage.getTitle());
        extendData.put("pageType", wikiPage.getPageType().name());
        extendData.put("pageTypeName", wikiPage.getPageType() == PageType.DIRECTORY ? "目录" : "文档");
        extendData.put("path", wikiPage.getPath());
        extendData.put("projectId", wikiPage.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("operatorId", operatorId);
        extendData.put("operatorName", operatorName);
        extendData.put("redirectUrl", "/wiki/project/" + wikiPage.getProjectId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建任务提醒扩展数据JSON
     */
    private String buildTaskReminderExtendData(Task task, Long receiverId, String receiverName, String projectName, String timeDesc) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("taskId", task.getId());
        extendData.put("taskTitle", task.getTitle());
        extendData.put("projectId", task.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("dueDate", task.getDueDate());
        extendData.put("priority", task.getPriority().name());
        extendData.put("priorityName", task.getPriority().name());
        extendData.put("creatorId", task.getCreatorId());
        extendData.put("creatorName", getOperatorName(task.getCreatorId()));
        extendData.put("receiverId", receiverId);
        extendData.put("receiverName", receiverName);
        extendData.put("reminderTimeDesc", timeDesc);
        extendData.put("redirectUrl", "/tasks/" + task.getId());
        return JsonUtils.toJsonString(extendData);
    }

    /**
     * 构建任务逾期扩展数据JSON
     */
    private String buildTaskOverdueExtendData(Task task, Long receiverId, String receiverName, String projectName) {
        Map<String, Object> extendData = new HashMap<>();
        extendData.put("taskId", task.getId());
        extendData.put("taskTitle", task.getTitle());
        extendData.put("projectId", task.getProjectId());
        extendData.put("projectName", projectName);
        extendData.put("dueDate", task.getDueDate());
        extendData.put("priority", task.getPriority().name());
        extendData.put("priorityName", task.getPriority().name());
        extendData.put("creatorId", task.getCreatorId());
        extendData.put("creatorName", getOperatorName(task.getCreatorId()));
        extendData.put("receiverId", receiverId);
        extendData.put("receiverName", receiverName);
        extendData.put("redirectUrl", "/tasks/" + task.getId());
        return JsonUtils.toJsonString(extendData);
    }
}
