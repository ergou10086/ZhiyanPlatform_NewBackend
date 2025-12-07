package hbnu.project.zhiyanbackend.basic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring定时任务配置
 * 启用定时任务功能
 *
 * @author ErgouTree
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // 启用 @Scheduled 注解支持
}