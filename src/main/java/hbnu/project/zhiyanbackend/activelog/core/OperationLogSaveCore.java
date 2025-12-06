package hbnu.project.zhiyanbackend.activelog.core;

import hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.AchievementOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.ProjectOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.TaskOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.WikiOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.enums.AchievementOperationType;
import hbnu.project.zhiyanbackend.activelog.model.enums.BizOperationModule;
import hbnu.project.zhiyanbackend.activelog.model.enums.ProjectOperationType;
import hbnu.project.zhiyanbackend.activelog.model.enums.TaskOperationType;
import hbnu.project.zhiyanbackend.activelog.model.enums.WikiOperationType;
import hbnu.project.zhiyanbackend.activelog.repository.AchievementOperationLogRepository;
import hbnu.project.zhiyanbackend.activelog.repository.ProjectOperationLogRepository;
import hbnu.project.zhiyanbackend.activelog.repository.TaskOperationLogRepository;
import hbnu.project.zhiyanbackend.activelog.repository.WikiOperationLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 操作日志的持久化核心类
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogSaveCore {

    private final ProjectOperationLogRepository projectOperationLogRepository;
    private final TaskOperationLogRepository taskOperationLogRepository;
    private final WikiOperationLogRepository wikiOperationLogRepository;
    private final AchievementOperationLogRepository achievementOperationLogRepository;

    /**
     * 操作时间字段名
     */
    private static final String OPERATION_TIME_FIELD = "operationTime";


    /**
     * 根据业务模块保存日志
     */
    @Transactional
    public void saveLogByModule(
            BizOperationLog annotation,
            Long projectId,
            Long bizId,
            String bizTitle,
            Long userId,
            String username,
            LocalDateTime operationTime) {
        try {
            // 使用 if-else 替代 switch，避免可能的匿名内部类生成问题
            BizOperationModule module = annotation.module();
            if (module == BizOperationModule.PROJECT) {
                saveProjectLog(annotation, projectId, bizTitle, userId, username, operationTime);
            } else if (module == BizOperationModule.TASK) {
                saveTaskLog(annotation, projectId, bizId, bizTitle, userId, username, operationTime);
            } else if (module == BizOperationModule.WIKI) {
                saveWikiLog(annotation, projectId, bizId, bizTitle, userId, username, operationTime);
            } else if (module == BizOperationModule.ACHIEVEMENT) {
                saveAchievementLog(annotation, projectId, bizId, bizTitle, userId, username, operationTime);
            } else {
                log.warn("未知的业务模块: {}", annotation.module());
            }
        }catch (Exception e) {
            log.error("保存操作日志失败: module={}, type={}, error={}",
                    annotation.module(), annotation.type(), e.getMessage(), e);
        }
    }


    /**
     * 保存项目操作日志
     */
    private void saveProjectLog(BizOperationLog annotation,
                                Long projectId,
                                String projectName,
                                Long userId,
                                String username,
                                LocalDateTime operationTime){
        ProjectOperationType operationType;
        try {
            operationType = ProjectOperationType.valueOf(annotation.type());
        } catch (IllegalArgumentException e) {
            log.warn("无效的项目操作类型: {}", annotation.type());
            return;
        }

        // 如果projectId为null，记录警告但不阻止保存
        if (projectId == null) {
            log.warn("保存项目操作日志时projectId为null: type={}, userId={}, username={}", 
                    annotation.type(), userId, username);
        }
        
        // 如果projectName为null，使用默认值
        if (projectName == null || projectName.trim().isEmpty()) {
            projectName = "未知项目";
            log.warn("保存项目操作日志时projectName为null，使用默认值");
        }

        ProjectOperationLog operationLog = ProjectOperationLog.builder()
                .projectId(projectId)
                .projectName(projectName)
                .userId(userId)
                .username(username)
                .operationType(operationType)
                .operationModule("项目管理")
                .operationDesc(annotation.description())
                .operationTime(operationTime)
                .build();

        projectOperationLogRepository.save(operationLog);
        log.info("项目操作日志保存成功: projectId={}, projectName={}, type={}",
                projectId, projectName, operationType);
    }


    /**
     * 保存任务操作日志
     */
    private void saveTaskLog(
            BizOperationLog annotation,
            Long projectId,
            Long taskId,
            String taskTitle,
            Long userId,
            String username,
            LocalDateTime operationTime){

        TaskOperationType operationType;
        try {
            operationType = TaskOperationType.valueOf(annotation.type());
        } catch (IllegalArgumentException e) {
            log.warn("无效的任务操作类型: {}", annotation.type());
            return;
        }

        // 如果关键信息缺失，记录警告
        if (projectId == null) {
            log.warn("保存任务操作日志时projectId为null: type={}, taskId={}, userId={}", 
                    annotation.type(), taskId, userId);
        }
        if (taskId == null) {
            log.warn("保存任务操作日志时taskId为null: type={}, projectId={}, userId={}", 
                    annotation.type(), projectId, userId);
        }
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = "未知任务";
            log.warn("保存任务操作日志时taskTitle为null，使用默认值");
        }

        TaskOperationLog operationLog = TaskOperationLog.builder()
                .projectId(projectId)
                .taskId(taskId)
                .taskTitle(taskTitle)
                .userId(userId)
                .username(username)
                .operationType(operationType)
                .operationModule("任务管理")
                .operationDesc(annotation.description())
                .operationTime(operationTime)
                .build();

        taskOperationLogRepository.save(operationLog);
        log.info("任务操作日志保存成功: projectId={}, taskId={}, taskTitle={}, type={}", 
                projectId, taskId, taskTitle, operationType);
    }


    /**
     * 保存Wiki操作日志
     */
    private void saveWikiLog(
            BizOperationLog annotation,
            Long projectId,
            Long wikiPageId,
            String wikiPageTitle,
            Long userId,
            String username,
            LocalDateTime operationTime){

        WikiOperationType operationType;
        try {
            operationType = WikiOperationType.valueOf(annotation.type());
        } catch (IllegalArgumentException e) {
            log.warn("无效的Wiki操作类型: {}", annotation.type());
            return;
        }

        WikiOperationLog operationLog = WikiOperationLog.builder()
                .projectId(projectId)
                .wikiPageId(wikiPageId)
                .wikiPageTitle(wikiPageTitle)
                .userId(userId)
                .username(username)
                .operationType(operationType)
                .operationModule("知识库Wiki管理")
                .operationDesc(annotation.description())
                .operationTime(operationTime)
                .build();

        wikiOperationLogRepository.save(operationLog);
    }


    /**
     * 保存成果操作日志
     */
    private void saveAchievementLog(
            BizOperationLog annotation,
            Long projectId,
            Long achievementId,
            String achievementTitle,
            Long userId,
            String username,
            LocalDateTime operationTime){

        AchievementOperationType operationType;
        try {
            operationType = AchievementOperationType.valueOf(annotation.type());
        } catch (IllegalArgumentException e) {
            log.warn("无效的成果操作类型: {}", annotation.type());
            return;
        }

        AchievementOperationLog operationLog = AchievementOperationLog.builder()
                .projectId(projectId)
                .achievementId(achievementId)
                .achievementTitle(achievementTitle)
                .userId(userId)
                .username(username)
                .operationType(operationType)
                .operationModule("成果管理")
                .operationDesc(annotation.description())
                .operationTime(operationTime)
                .build();

        achievementOperationLogRepository.save(operationLog);
        log.info("成果操作日志保存成功: projectId={}, achievementId={}, achievementTitle={}, type={}", 
                projectId, achievementId, achievementTitle, operationType);
    }

}
