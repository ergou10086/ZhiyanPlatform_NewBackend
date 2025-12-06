package hbnu.project.zhiyanbackend.activelog.core;

import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static hbnu.project.zhiyanbackend.basic.utils.FileUtils.formatFileSize;

/**
 * 操作日志设置辅助类
 * 减少在controller中大量编写操作日志记录的相关逻辑
 * 而且可以自己选择操作日志的业务逻辑放到控制器中还是服务层中（一般放到控制器层，控制器操作成功之后才记录日志。而且记录操作日志不能阻塞业务流程）
 *
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogHelper {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // ==================== 项目操作日志 ====================

    /**
     * 记录创建项目日志
     *
     * @param projectId   项目ID
     * @param projectName 项目名称
     */
    public void logProjectCreate(Long projectId, String projectName) {
        try {
            OperationLogContext.setBasicInfo(projectId, projectId, projectName);
            log.debug("设置创建项目日志上下文: projectId={}, projectName={}", projectId, projectName);
        } catch (Exception e) {
            log.error("记录创建项目日志失败: projectId={}, error={}", projectId, e.getMessage(), e);
        }
    }

    /**
     * 记录更新项目日志
     *
     * @param projectId   项目ID
     * @param projectName 项目名称（更新后的）
     */
    public void logProjectUpdate(Long projectId, String projectName) {
        try {
            OperationLogContext.setBasicInfo(projectId, projectId, projectName);
            log.debug("设置更新项目日志上下文: projectId={}, projectName={}", projectId, projectName);
        } catch (Exception e) {
            log.error("记录更新项目日志失败: projectId={}, error={}", projectId, e.getMessage(), e);
        }
    }

    /**
     * 记录删除项目日志
     *
     * @param projectId   项目ID
     * @param projectName 项目名称
     */
    public void logProjectDelete(Long projectId, String projectName) {
        try {
            OperationLogContext.setBasicInfo(projectId, projectId, projectName);
            log.debug("设置删除项目日志上下文: projectId={}, projectName={}", projectId, projectName);
        } catch (Exception e) {
            log.error("记录删除项目日志失败: projectId={}, error={}", projectId, e.getMessage(), e);
        }
    }

    /**
     * 记录项目状态变更日志
     *
     * @param projectId   项目ID
     * @param projectName 项目名称
     * @param newStatus   新状态
     */
    public void logProjectStatusChange(Long projectId, String projectName, ProjectStatus newStatus) {
        try {
            OperationLogContext.setBasicInfo(projectId, projectId, projectName);
            String statusDesc = String.format("将项目状态更新为【%s】",
                    newStatus != null ? newStatus.getStatusName() : "未知");
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(statusDesc);
            }
            log.debug("设置项目状态变更日志上下文: projectId={}, newStatus={}", projectId, newStatus);
        } catch (Exception e) {
            log.error("记录项目状态变更日志失败: projectId={}, error={}", projectId, e.getMessage(), e);
        }
    }

    // ==================== 项目成员操作日志 ====================

    /**
     * 记录添加成员日志
     *
     * @param projectId  项目ID
     * @param userId     被添加的用户ID
     * @param role       成员角色
     */
    public void logMemberAdd(Long projectId, Long userId, ProjectMemberRole role) {
        try {
            String projectName = getProjectName(projectId);
            String username = getUsername(userId);

            OperationLogContext.setBasicInfo(projectId, projectId, projectName);

            String roleDesc = role != null ? role.getRoleName() : "未知角色";
            String operationDesc = String.format("添加成员【%s】到项目【%s】，角色为【%s】",
                    username, projectName, roleDesc);

            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }

            log.debug("设置添加成员日志上下文: projectId={}, userId={}, role={}",
                    projectId, userId, role);
        } catch (Exception e) {
            log.error("记录添加成员日志失败: projectId={}, userId={}, error={}",
                    projectId, userId, e.getMessage(), e);
        }
    }

    /**
     * 记录移除成员日志
     *
     * @param projectId  项目ID
     * @param userId     被移除的用户ID
     */
    public void logMemberRemove(Long projectId, Long userId) {
        try{
            String projectName = getProjectName(projectId);
            String username = getUsername(userId);

            OperationLogContext.setBasicInfo(projectId, projectId, projectName);

            String operationDesc = String.format("从项目【%s】中移除成员【%s】",
                    projectName, username);

            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }

            log.debug("设置移除成员日志上下文: projectId={}, userId={}", projectId, userId);
        }catch (Exception e){
            log.error("记录移除成员日志失败: projectId={}, userId={}, error={}", projectId, userId, e.getMessage(), e);
        }
    }

    /**
     * 记录角色变更日志
     *
     * @param projectId  项目ID
     * @param userId     用户ID
     * @param oldRole    原角色
     * @param newRole    新角色
     */
    public void logRoleChange(Long projectId, Long userId, ProjectMemberRole oldRole, ProjectMemberRole newRole) {
        try {
            String projectName = getProjectName(projectId);
            String username = getUsername(userId);

            OperationLogContext.setBasicInfo(projectId, projectId, projectName);

            String oldRoleDesc = oldRole != null ? oldRole.getRoleName() : "未知";
            String newRoleDesc = newRole != null ? newRole.getRoleName() : "未知";
            String operationDesc = String.format("将成员【%s】在项目【%s】中的角色从【%s】变更为【%s】",
                    username, projectName, oldRoleDesc, newRoleDesc);

            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }

            log.debug("设置角色变更日志上下文: projectId={}, userId={}, oldRole={}, newRole={}",
                    projectId, userId, oldRole, newRole);
        } catch (Exception e) {
            log.error("记录角色变更日志失败: projectId={}, userId={}, error={}",
                    projectId, userId, e.getMessage(), e);
        }
    }

    // ==================== 任务操作日志 ====================

    /**
     * 记录创建任务日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     */
    public void logTaskCreate(Long projectId, Long taskId, String taskTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            log.debug("设置创建任务日志上下文: projectId={}, taskId={}, title={}",
                    projectId, taskId, taskTitle);
        } catch (Exception e) {
            log.error("记录创建任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录更新任务日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     */
    public void logTaskUpdate(Long projectId, Long taskId, String taskTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            log.debug("设置更新任务日志上下文: projectId={}, taskId={}, title={}",
                    projectId, taskId, taskTitle);
        } catch (Exception e) {
            log.error("记录更新任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录删除任务日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     */
    public void logTaskDelete(Long projectId, Long taskId, String taskTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            log.debug("设置删除任务日志上下文: projectId={}, taskId={}, title={}",
                    projectId, taskId, taskTitle);
        } catch (Exception e) {
            log.error("记录删除任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录分配任务日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     */
    public void logTaskAssign(Long projectId, Long taskId, String taskTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            log.debug("设置分配任务日志上下文: projectId={}, taskId={}, title={}",
                    projectId, taskId, taskTitle);
        } catch (Exception e) {
            log.error("记录分配任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录分配任务日志（带分配人列表）
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     * @param assigneeIds 被分配的用户ID列表
     */
    public void logTaskAssign(Long projectId, Long taskId, String taskTitle, List<Long> assigneeIds) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            String assigneeNames = assigneeIds != null && !assigneeIds.isEmpty()
                    ? assigneeIds.stream()
                    .map(this::getUsername)
                    .collect(Collectors.joining("、"))
                    : "无";
            String operationDesc = String.format("分配任务【%s】给【%s】", taskTitle, assigneeNames);
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置分配任务日志上下文: projectId={}, taskId={}, assigneeIds={}",
                    projectId, taskId, assigneeIds);
        } catch (Exception e) {
            log.error("记录分配任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录提交任务日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     */
    public void logTaskSubmit(Long projectId, Long taskId, String taskTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            log.debug("设置提交任务日志上下文: projectId={}, taskId={}, title={}",
                    projectId, taskId, taskTitle);
        } catch (Exception e) {
            log.error("记录提交任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录任务状态变更日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     * @param newStatus   新状态
     */
    public void logTaskStatusChange(Long projectId, Long taskId, String taskTitle, Object newStatus) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            log.debug("设置任务状态变更日志上下文: projectId={}, taskId={}, newStatus={}",
                    projectId, taskId, newStatus);
        } catch (Exception e) {
            log.error("记录任务状态变更日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录完成任务日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     */
    public void logTaskComplete(Long projectId, Long taskId, String taskTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            log.debug("设置完成任务日志上下文: projectId={}, taskId={}, title={}",
                    projectId, taskId, taskTitle);
        } catch (Exception e) {
            log.error("记录完成任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 记录审核任务日志
     *
     * @param projectId   项目ID
     * @param taskId      任务ID
     * @param taskTitle   任务标题
     * @param reviewResult 审核结果（通过/拒绝）
     */
    public void logTaskReview(Long projectId, Long taskId, String taskTitle, String reviewResult) {
        try {
            OperationLogContext.setBasicInfo(projectId, taskId, taskTitle);
            String operationDesc = String.format("审核任务【%s】，结果：%s", taskTitle, reviewResult);
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置审核任务日志上下文: projectId={}, taskId={}, reviewResult={}",
                    projectId, taskId, reviewResult);
        } catch (Exception e) {
            log.error("记录审核任务日志失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    // ==================== Wiki操作日志 ====================

    /**
     * 记录创建Wiki页面日志
     *
     * @param projectId   项目ID
     * @param wikiPageId  Wiki页面ID
     * @param wikiPageTitle Wiki页面标题
     */
    public void logWikiCreate(Long projectId, Long wikiPageId, String wikiPageTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, wikiPageId, wikiPageTitle);
            log.debug("设置创建Wiki页面日志上下文: projectId={}, wikiPageId={}, title={}",
                    projectId, wikiPageId, wikiPageTitle);
        } catch (Exception e) {
            log.error("记录创建Wiki页面日志失败: wikiPageId={}, error={}", wikiPageId, e.getMessage(), e);
        }
    }

    /**
     * 记录更新Wiki页面日志
     *
     * @param projectId   项目ID
     * @param wikiPageId  Wiki页面ID
     * @param wikiPageTitle Wiki页面标题
     */
    public void logWikiUpdate(Long projectId, Long wikiPageId, String wikiPageTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, wikiPageId, wikiPageTitle);
            log.debug("设置更新Wiki页面日志上下文: projectId={}, wikiPageId={}, title={}",
                    projectId, wikiPageId, wikiPageTitle);
        } catch (Exception e) {
            log.error("记录更新Wiki页面日志失败: wikiPageId={}, error={}", wikiPageId, e.getMessage(), e);
        }
    }

    /**
     * 记录删除Wiki页面日志
     *
     * @param projectId   项目ID
     * @param wikiPageId  Wiki页面ID
     * @param wikiPageTitle Wiki页面标题
     */
    public void logWikiDelete(Long projectId, Long wikiPageId, String wikiPageTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, wikiPageId, wikiPageTitle);
            log.debug("设置删除Wiki页面日志上下文: projectId={}, wikiPageId={}, title={}",
                    projectId, wikiPageId, wikiPageTitle);
        } catch (Exception e) {
            log.error("记录删除Wiki页面日志失败: wikiPageId={}, error={}", wikiPageId, e.getMessage(), e);
        }
    }

    /**
     * 记录移动Wiki页面日志
     *
     * @param projectId   项目ID
     * @param wikiPageId  Wiki页面ID
     * @param wikiPageTitle Wiki页面标题
     * @param newParentId 新父页面ID
     */
    public void logWikiMove(Long projectId, Long wikiPageId, String wikiPageTitle, Long newParentId) {
        try {
            OperationLogContext.setBasicInfo(projectId, wikiPageId, wikiPageTitle);
            String operationDesc = String.format("移动Wiki页面【%s】到父页面【%s】", 
                    wikiPageTitle, newParentId != null ? String.valueOf(newParentId) : "根目录");
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置移动Wiki页面日志上下文: projectId={}, wikiPageId={}, newParentId={}",
                    projectId, wikiPageId, newParentId);
        } catch (Exception e) {
            log.error("记录移动Wiki页面日志失败: wikiPageId={}, error={}", wikiPageId, e.getMessage(), e);
        }
    }

    // ==================== 成果操作日志 ====================

    /**
     * 记录创建成果日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     */
    public void logAchievementCreate(Long projectId, Long achievementId, String achievementTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            log.debug("设置创建成果日志上下文: projectId={}, achievementId={}, title={}",
                    projectId, achievementId, achievementTitle);
        } catch (Exception e) {
            log.error("记录创建成果日志失败: achievementId={}, error={}", achievementId, e.getMessage(), e);
        }
    }

    /**
     * 记录更新成果状态日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     * @param newStatus       新状态
     */
    public void logAchievementStatusUpdate(Long projectId, Long achievementId, String achievementTitle, String newStatus) {
        try {
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            String statusDesc = String.format("将成果【%s】的状态更新为【%s】", achievementTitle, newStatus != null ? newStatus : "未知成果");

            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(statusDesc);
            }
            log.debug("设置成果状态更新日志上下文: achievementId={}, newStatus={}",
                    achievementId, newStatus);
        }catch (Exception e) {
            log.error("记录成果状态更新日志失败: achievementId={}, error={}",
                    achievementId, e.getMessage(), e);
        }
    }

    /**
     * 记录更新成果详情日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     */
    public void logAchievementDetailUpdate(Long projectId, Long achievementId, String achievementTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            String operationDesc = String.format("更新成果【%s】的详情信息", achievementTitle);

            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置成果详情更新日志上下文: achievementId={}", achievementId);
        }catch (Exception e) {
            log.error("记录成果详情更新日志失败: achievementId={}, error={}",
                    achievementId, e.getMessage(), e);
        }
    }

    /**
     * 记录删除成果日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     */
    public void logAchievementDelete(Long projectId, Long achievementId, String achievementTitle) {
        try {
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            String operationDesc = String.format("删除成果【%s】", achievementTitle);
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置删除成果日志上下文: achievementId={}", achievementId);
        }catch (Exception e) {
            log.error("记录删除成果日志失败: achievementId={}, error={}",
                    achievementId, e.getMessage(), e);
        }
    }

    /**
     * 记录文件上传日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     * @param fileName        文件名
     * @param fileSize        文件大小（字节）
     */
    public void logAchievementFileUpload(Long projectId, Long achievementId,
                                         String achievementTitle, String fileName, Long fileSize) {
        try{
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            String sizeDesc = formatFileSize(fileSize);
            String operationDesc = String.format("为成果【%s】上传文件【%s】，大小：%s",
                    achievementTitle, fileName, sizeDesc);
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置文件上传日志上下文: achievementId={}, fileName={}",
                    achievementId, fileName);
        }catch (Exception e){
            log.error("记录文件上传日志失败: achievementId={}, error={}",
                    achievementId, e.getMessage(), e);
        }
    }

    /**
     * 记录批量文件上传日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     * @param fileCount       文件数量
     */
    public void logAchievementBatchFileUpload(Long projectId, Long achievementId,
                                              String achievementTitle, int fileCount) {
        try {
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            String operationDesc = String.format("为成果【%s】批量上传 %d 个文件",
                    achievementTitle, fileCount);
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置批量文件上传日志上下文: achievementId={}, fileCount={}",
                    achievementId, fileCount);
        } catch (Exception e) {
            log.error("记录批量文件上传日志失败: achievementId={}, error={}",
                    achievementId, e.getMessage(), e);
        }
    }

    /**
     * 记录文件删除日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     * @param fileName        文件名
     */
    public void logAchievementFileDelete(Long projectId, Long achievementId,
                                         String achievementTitle, String fileName) {
        try {
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            String operationDesc = String.format("从成果【%s】中删除文件【%s】",
                    achievementTitle, fileName);
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置文件删除日志上下文: achievementId={}, fileName={}",
                    achievementId, fileName);
        } catch (Exception e) {
            log.error("记录文件删除日志失败: achievementId={}, error={}",
                    achievementId, e.getMessage(), e);
        }
    }

    /**
     * 记录批量文件删除日志
     *
     * @param projectId       项目ID
     * @param achievementId   成果ID
     * @param achievementTitle 成果标题
     * @param fileCount       删除的文件数量
     */
    public void logAchievementBatchFileDelete(Long projectId, Long achievementId,
                                              String achievementTitle, int fileCount) {
        try {
            OperationLogContext.setBasicInfo(projectId, achievementId, achievementTitle);
            String operationDesc = String.format("从成果【%s】中批量删除 %d 个文件",
                    achievementTitle, fileCount);
            OperationLogContext context = OperationLogContext.get();
            if (context != null) {
                context.setExtra(operationDesc);
            }
            log.debug("设置批量文件删除日志上下文: achievementId={}, fileCount={}",
                    achievementId, fileCount);
        } catch (Exception e) {
            log.error("记录批量文件删除日志失败: achievementId={}, error={}",
                    achievementId, e.getMessage(), e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取项目名称
     *
     * @param projectId 项目ID
     * @return 项目名称，如果未找到返回"未知项目"
     */
    private String getProjectName(Long projectId) {
        if (projectId == null) {
            return "未知项目";
        }
        try {
            return projectRepository.findProjectNameById(projectId)
                    .orElse("未知项目");
        } catch (Exception e) {
            log.warn("获取项目名称失败: projectId={}, error={}", projectId, e.getMessage());
            return "未知项目";
        }
    }

    /**
     * 获取用户名
     *
     * @param userId 用户ID
     * @return 用户名，如果未找到返回"未知用户"
     */
    private String getUsername(Long userId) {
        if (userId == null) {
            return "未知用户";
        }
        try {
            return userRepository.findNameById(userId)
                    .orElse("未知用户");
        } catch (Exception e) {
            log.warn("获取用户名失败: userId={}, error={}", userId, e.getMessage());
            return "未知用户";
        }
    }

    /**
     * 清理日志上下文
     * 注意：通常不需要手动调用，切面会自动清理，此为提醒后备类
     */
    public void clearContext() {
        OperationLogContext.clear();
    }
}
