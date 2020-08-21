package com.gin.ngabotchan.config;

import com.gin.ngabotchan.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 多线程执行定时任务
 * @author bx002
 */
@Configuration
@Slf4j
//所有的定时任务都放在一个线程池中，定时任务启动时使用不同都线程
public class ScheduleConfig implements SchedulingConfigurer {
    ThreadPoolTaskScheduler taskScheduler;


    @Bean(name = "myThreadPoolTaskScheduler")
    public TaskScheduler getMyThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("myScheduled-");
        taskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //调度器shutdown被调用时等待当前被调度的任务完成
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        //等待时长
        taskScheduler.setAwaitTerminationSeconds(60);
        return taskScheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
         taskScheduler = taskScheduler==null?SpringContextUtil.getBean(ThreadPoolTaskScheduler.class):taskScheduler;
        //设定一个长度10的定时任务线程池
        taskRegistrar.setScheduler(taskScheduler);
    }

}
