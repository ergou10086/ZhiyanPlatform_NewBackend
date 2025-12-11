package hbnu.project.zhiyanbackend.message.service;

import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskSubmission;

import java.util.List;
import java.util.Map;

/**
 * 消息发送服务
 *
 * @author ErgouTree
 */
public interface MessageSendService {

    /**
     * 发送成果文件上传的通知
     * 发送给除了上传者的所有项目成员
     *
     * @param achievement 成果实体
     * @param file        文件实体
     * @param uploaderId  上传者ID
     */
    void notifyAchievementFileUpload(Achievement achievement, AchievementFile file, Long uploaderId);

    /**
     * 发送批量文件上传通知
     * 发送给除了上传者的所有项目成员
     *
     * @param achievement 成果实体
     * @param files       上传的文件列表
     * @param uploaderId  上传者ID
     */
    void notifyAchievementFilesBatchUpload(Achievement achievement, List<AchievementFile> files, Long uploaderId);

    /**
     * 发送成果文件删除的通知
     *
     * @param achievement 成果实体
     * @param file        文件实体
     * @param operatorId  操作者ID
     */
    void notifyAchievementFileDeleted(Achievement achievement, AchievementFile file, Long operatorId);

    /**
     * 发送批量文件删除通知
     *
     * @param achievementMap 成果ID到成果实体的映射
     * @param files 被删除的文件列表
     * @param operatorId 操作者ID
     */
    void notifyAchievementFilesBatchDeleted(Map<Long, Achievement> achievementMap,
                                            List<AchievementFile> files, Long operatorId);

    /**
     * 发送成果所有文件被删除的通知
     *
     * @param achievement 成果实体
     * @param files 被删除的文件列表
     * @param operatorId 操作者ID
     */
    void notifyAchievementAllFilesDeleted(Achievement achievement,
                                          List<AchievementFile> files, Long operatorId);

    /**
     * 发送 更新成果状态 通知给项目成员
     * @param achievement 更改的成果
     * @param oldStatus 旧状态
     * @param status 新状态
     * @param operatorId 操作的用户id
     */
    void notifyAchievementStatusChange(Achievement achievement, AchievementStatus oldStatus, AchievementStatus status, Long operatorId);

    /**
     * 发送 成果创建的通知
     *
     * @param achievement 创建的成果
     */
    void notifyAchievementCreated(Achievement achievement);

    /**
     * 发送任务提交审核结果通知
     * 发送给任务提交者
     *
     * @param task 任务实体
     * @param submission 任务提交记录
     * @param reviewStatus 审核状态（APPROVED或REJECTED）
     * @param reviewerId 审核人ID
     */
    void notifyTaskSubmissionReviewed(hbnu.project.zhiyanbackend.tasks.model.entity.Task task,
                                       hbnu.project.zhiyanbackend.tasks.model.entity.TaskSubmission submission,
                                       hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus reviewStatus,
                                       Long reviewerId);

    /**
     * 发送Wiki页面创建通知
     * 发送给除了创建者的所有项目成员
     *
     * @param wikiPage Wiki页面实体
     * @param creatorId 创建者ID
     */
    void notifyWikiPageCreated(hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage wikiPage, Long creatorId);

    /**
     * 发送Wiki页面更新通知
     * 发送给除了编辑者的所有项目成员
     *
     * @param wikiPage Wiki页面实体
     * @param editorId 编辑者ID
     * @param changeDesc 修改说明
     */
    void notifyWikiPageUpdated(hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage wikiPage, Long editorId, String changeDesc);

    /**
     * 发送Wiki页面删除通知
     * 发送给除了删除者的所有项目成员
     *
     * @param wikiPage Wiki页面实体
     * @param operatorId 操作者ID
     */
    void notifyWikiPageDeleted(hbnu.project.zhiyanbackend.wiki.model.entity.WikiPage wikiPage, Long operatorId);

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
    void notifyAccountSecurityAlert(Long userId, String currentIp, String currentLocation, String lastIp, String lastLocation);

    /**
     * 发送邮箱修改通知
     * 发送给用户自己（高优先级系统消息）
     *
     * @param userId 用户ID
     * @param oldEmail 旧邮箱
     * @param newEmail 新邮箱
     */
    void notifyEmailChanged(Long userId, String oldEmail, String newEmail);

    /**
     * 发送任务提醒通知
     * 从任务截止的那天那小时开始算，每截止前72小时，48小时，24小时发送一次任务提醒通知
     *
     * @param task 消息针对的任务
     * @param receiverId 接收人
     */
    void notifyTaskNeedSubmission(Task task, Long receiverId);

    /**
     * 发送任务逾期通知
     * 任务截止的那天开始，如果任务没有提交，发送一条任务逾期警告
     *
     * @param task 消息针对的任务
     * @param receiverId 接收人
     */
    void notifyTaskOverSubmissionTime(Task task, Long receiverId, long overdueDays);

    /**
     * 发送待审核任务通知
     * 当任务被提交后，通知任务创建者（审核者）有新的待审核任务
     *
     * @param task 任务实体
     * @param submission 任务提交记录
     * @param submitterId 提交者ID
     */
    void notifyTaskReviewRequest(Task task, TaskSubmission submission, Long submitterId);

    /**
     * 发送成果删除通知
     * 发送给除了删除者的所有项目成员
     *
     * @param achievement 被删除的成果实体
     * @param operatorId 操作者ID
     */
    void notifyAchievementDeleted(Achievement achievement, Long operatorId);
}
