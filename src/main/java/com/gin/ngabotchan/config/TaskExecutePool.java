package com.gin.ngabotchan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池
 */
@Configuration
@EnableAsync
public class TaskExecutePool {
    final static Integer QUEUE = 1000;
    final static Integer KEEPALIVE = 300;

    @Bean
    public Executor scanExecutor() {
        return getExecutor("scan-", 15, 15, QUEUE, KEEPALIVE);
    }

    @Bean
    public Executor downloadExecutor() {
        return getExecutor("scan-", 15, 15, QUEUE, KEEPALIVE);
    }


    /**
     * 创建线程池
     *
     * @param name      线程池名称
     * @param coreSize  核心线程池大小
     * @param poolSize  最大线程数
     * @param queue     队列容量
     * @param keepAlive 活跃时间
     * @return 线程池
     */
    private Executor getExecutor(String name, Integer coreSize, Integer poolSize, Integer queue, Integer keepAlive) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(coreSize);
        //最大线程数
        executor.setMaxPoolSize(poolSize);
        //队列容量
        executor.setQueueCapacity(queue);
        //活跃时间
        executor.setKeepAliveSeconds(keepAlive);
        //线程名字前缀
        executor.setThreadNamePrefix(name);

        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
