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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

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

    // 提醒时间点（小时数）: 任务截止前 72, 48, 24 小时
    private static final Set<Long> REMINDER_HOURS = Set.of(72L, 48L, 24L);

    /**
     * 每小时整点执行一次，检查任务并发送提醒通知
     * 在任务截止前72小时、48小时、24小时发送提醒
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void sendTaskReminders() {
        // 为了精确到小时，使用 LocalDateTime
        LocalDateTime now = LocalDateTime.now();
        int page = 0;
        int pageSize = 100;
        Pageable pageable = PageRequest.of(page, pageSize);
        long totalRemindersSent = 0;

        try {
            Page<Task> taskPage;
            
            do {
                // 查询所有查询未完成且有截止日期的任务，而且未删除，也就是状态为 TODO 或 IN_PROGRESS，isDeleted=false，且 dueDate 在当前时间之后的任务
                taskPage = taskRepository.findTasksForReminder(now, pageable);

                List<Task> activeTasks = taskPage.getContent();

                if (activeTasks.isEmpty()) {
                    break;
                }
                
                for (Task task : activeTasks) {
                    // 只处理有截止日期且状态为进行中的任务
                    if (task.getDueDate() == null) {
                        continue;
                    }

                    // 检查距离截止日期的剩余小时数
                    long remainingHours = ChronoUnit.HOURS.between(now, task.getDueDate());

                    // 只处理截止日期在未来的任务 (remainingHours > 0)
                    if (remainingHours <= 0) {
                        continue;
                    }
                    
                    // 只在72小时、48小时、24小时时发送提醒
                    long reminderHour = REMINDER_HOURS.stream()
                            .filter(h -> remainingHours > h && remainingHours <= h + 1)
                            .findFirst()
                            .orElse(-1L);

                    if (reminderHour != -1L) {
                        // 获取任务的所有执行者
                        List<TaskUser> executors = taskUserRepository.findActiveExecutorsByTaskId(task.getId());
                        if (!executors.isEmpty()) {
                            // 消息描述
                            String timeDesc = reminderHour + "小时";
                            for (TaskUser executor : executors) {
                                try {
                                    messageSendService.notifyTaskNeedSubmission(task, executor.getUserId());
                                    totalRemindersSent++;
                                } catch (Exception e) {
                                    log.error("发送任务提醒通知失败: taskId={}, userId={}", 
                                            task.getId(), executor.getUserId(), e);
                                }
                            }
                        }
                    }
                }
                
                page++;
                pageable = PageRequest.of(page, pageSize);
            // 限制最大页数，避免无限循环
            } while (taskPage.hasNext() && page < 1000);
            
            log.info("任务提醒定时任务执行完成: 发送提醒{}条",totalRemindersSent);
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

