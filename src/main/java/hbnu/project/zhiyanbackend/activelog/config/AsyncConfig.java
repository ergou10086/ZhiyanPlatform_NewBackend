package hbnu.project.zhiyanbackend.activelog.config;

import hbnu.project.zhiyanbackend.activelog.core.OperationLogContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置类
 *
 * @author ErgouTree
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 操作日志专用线程池
     * 独立线程池避免影响业务线程
     * 配置TaskDecorator以传递ThreadLocal上下文
     */
    @Bean("operationLogTaskExecutor")
    public Executor operationLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(4);
        // 最大线程数
        executor.setMaxPoolSize(8);
        // 队列容量
        executor.setQueueCapacity(500);
        // 线程名前缀
        executor.setThreadNamePrefix("log-async-");
        // 空闲线程存活时间
        executor.setKeepAliveSeconds(60);

        // 配置TaskDecorator以传递ThreadLocal上下文
        executor.setTaskDecorator(new OperationLogContextTaskDecorator());

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * TaskDecorator实现，用于在异步执行时传递ThreadLocal上下文
     */
    private static class OperationLogContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 在提交任务时捕获当前线程的上下文
            OperationLogContext context = OperationLogContext.get();
            return () -> {
                try {
                    // 在异步线程中恢复上下文
                    if (context != null) {
                        OperationLogContext.set(context);
                    }
                    runnable.run();
                } finally {
                    // 清理异步线程的上下文
                    OperationLogContext.clear();
                }
            };
        }
    }
}