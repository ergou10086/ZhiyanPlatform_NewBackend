package hbnu.project.zhiyanbackend.tasks.scheduler;

import hbnu.project.zhiyanbackend.message.service.MessageSendService;
import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import hbnu.project.zhiyanbackend.tasks.model.entity.TaskUser;
import hbnu.project.zhiyanbackend.tasks.model.enums.ReviewStatus;
import hbnu.project.zhiyanbackend.tasks.model.enums.TaskStatus;
import hbnu.project.zhiyanbackend.tasks.repository.TaskRepository;
import hbnu.project.zhiyanbackend.tasks.repository.TaskSubmissionRepository;
import hbnu.project.zhiyanbackend.tasks.repository.TaskUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 任务提醒定时任务
 * 定期检查任务并发送提醒通知和逾期通知
 *
 * @author ErgouTree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskReminderScheduler {

    private final TaskRepository taskRepository;
    private final TaskUserRepository taskUserRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final MessageSendService messageSendService;

    /**
     * 每小时整点执行一次，检查任务并发送提醒通知
     * 在任务截止前72小时、48小时、24小时发送提醒
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void sendTaskReminders() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate threeDaysLater = today.plusDays(3);
            
            // 查询所有进行中的任务（TODO 和 IN_PROGRESS 状态）
            // 由于需要查询所有任务，使用分页批量处理
            int pageSize = 100;
            int page = 0;
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<Task> taskPage;
            int totalProcessed = 0;
            int totalReminders = 0;
            
            do {
                // 查询所有任务（使用JPA的findAll，然后过滤）
                taskPage = taskRepository.findAll(pageable);
                
                // 过滤出未删除的任务
                List<Task> activeTasks = taskPage.getContent().stream()
                        .filter(task -> !Boolean.TRUE.equals(task.getIsDeleted()))
                        .toList();
                
                if (activeTasks.isEmpty()) {
                    page++;
                    pageable = PageRequest.of(page, pageSize);
                    continue;
                }
                
                for (Task task : activeTasks) {
                    // 只处理有截止日期且状态为进行中的任务
                    if (task.getDueDate() == null) {
                        continue;
                    }
                    
                    // 只处理进行中的任务（TODO 和 IN_PROGRESS）
                    if (task.getStatus() != TaskStatus.TODO && task.getStatus() != TaskStatus.IN_PROGRESS) {
                        continue;
                    }
                    
                    // 检查是否在提醒时间范围内（今天到3天后）
                    if (task.getDueDate().isBefore(today) || task.getDueDate().isAfter(threeDaysLater)) {
                        continue;
                    }
                    
                    // 计算距离截止日期的小时数
                    long hoursUntilDue = ChronoUnit.HOURS.between(today.atStartOfDay(), task.getDueDate().atStartOfDay());
                    
                    // 只在72小时、48小时、24小时时发送提醒
                    if (hoursUntilDue == 72 || hoursUntilDue == 48 || hoursUntilDue == 24) {
                        // 获取任务的所有执行者
                        List<TaskUser> executors = taskUserRepository.findActiveExecutorsByTaskId(task.getId());
                        if (!executors.isEmpty()) {
                            for (TaskUser executor : executors) {
                                try {
                                    messageSendService.notifyTaskNeedSubmission(task, executor.getUserId());
                                    totalReminders++;
                                } catch (Exception e) {
                                    log.error("发送任务提醒通知失败: taskId={}, userId={}", 
                                            task.getId(), executor.getUserId(), e);
                                }
                            }
                        }
                    }
                    totalProcessed++;
                }
                
                page++;
                pageable = PageRequest.of(page, pageSize);
            // 限制最大页数，避免无限循环
            } while (taskPage.hasNext() && page < 1000);
            
            log.info("任务提醒定时任务执行完成: 处理任务{}个, 发送提醒{}条", totalProcessed, totalReminders);
        } catch (Exception e) {
            log.error("执行任务提醒定时任务时发生错误", e);
        }
    }

    /**
     * 每天执行一次，检查逾期任务并发送逾期通知
     * 任务截止后如果未提交，发送逾期警告，但是只发送三天
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天上午9点执行
    @Transactional(rollbackFor = Exception.class)
    public void sendTaskOverdueNotifications() {
        try {
            LocalDate today = LocalDate.now();
            
            // 查询所有逾期且未完成的任务
            // 由于需要查询所有项目，使用分页批量处理
            int pageSize = 100;
            int page = 0;
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<Task> taskPage;
            int totalProcessed = 0;
            int totalOverdueNotifications = 0;
            
            do {
                // 查询所有任务（使用JPA的findAll，然后过滤）
                taskPage = taskRepository.findAll(pageable);
                
                // 过滤出未删除的任务
                List<Task> activeTasks = taskPage.getContent().stream()
                        .filter(task -> !Boolean.TRUE.equals(task.getIsDeleted()))
                        .toList();
                
                if (activeTasks.isEmpty()) {
                    page++;
                    pageable = PageRequest.of(page, pageSize);
                    continue;
                }
                
                for (Task task : activeTasks) {
                    // 只处理有截止日期且已逾期的任务
                    if (task.getDueDate() == null || !task.getDueDate().isBefore(today)) {
                        continue;
                    }
                    
                    // 只处理进行中的任务（TODO 和 IN_PROGRESS）
                    if (task.getStatus() != TaskStatus.TODO && task.getStatus() != TaskStatus.IN_PROGRESS) {
                        continue;
                    }
                    
                    // 检查任务是否有已审核通过的提交
                    // 如果有已审核通过的提交，说明任务已完成，不需要发送逾期通知
                    boolean hasApprovedSubmission = taskSubmissionRepository
                            .findByTaskIdAndIsDeletedFalseOrderByVersionDesc(task.getId())
                            .stream()
                            .anyMatch(submission -> submission.getReviewStatus() != null 
                                    && submission.getReviewStatus() == ReviewStatus.APPROVED);
                    
                    if (hasApprovedSubmission) {
                        continue;
                    }
                    
                    // 获取任务的所有执行者
                    List<TaskUser> executors = taskUserRepository.findActiveExecutorsByTaskId(task.getId());
                    if (!executors.isEmpty()) {
                        for (TaskUser executor : executors) {
                            try {
                                // 计算逾期天数,如果逾期大于三天就不发送了
                                long overdueDays = ChronoUnit.DAYS.between(task.getDueDate(), LocalDate.now());
                                if(overdueDays <= 3) {
                                    messageSendService.notifyTaskOverSubmissionTime(task, executor.getUserId(), overdueDays);
                                    totalOverdueNotifications++;
                                }
                            } catch (Exception e) {
                                log.error("发送任务逾期通知失败: taskId={}, userId={}", 
                                        task.getId(), executor.getUserId(), e);
                            }
                        }
                    }
                    totalProcessed++;
                }
                
                page++;
                pageable = PageRequest.of(page, pageSize);
            // 限制最大页数，避免无限循环
            } while (taskPage.hasNext() && page < 1000);
            log.info("任务逾期通知定时任务执行完成: 处理任务{}个, 发送逾期通知{}条", 
                    totalProcessed, totalOverdueNotifications);
        } catch (Exception e) {
            log.error("执行任务逾期通知定时任务时发生错误", e);
        }
    }
}

