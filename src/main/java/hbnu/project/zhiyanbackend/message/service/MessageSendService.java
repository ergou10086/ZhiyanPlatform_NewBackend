package hbnu.project.zhiyanbackend.message.service;

import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;

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
}
