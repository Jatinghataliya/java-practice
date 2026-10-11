package com.example.excelexport.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${app.export.thread-pool.core-pool-size:4}")
    private int corePoolSize;

    @Value("${app.export.thread-pool.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${app.export.thread-pool.queue-capacity:50}")
    private int queueCapacity;

    @Value("${app.export.thread-pool.thread-name-prefix:ExcelExport-}")
    private String threadNamePrefix;

    @Bean(name = "excelExportExecutor")
    public Executor excelExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // CallerRunsPolicy or AbortPolicy with custom handler prevents silent task dropping
        executor.setRejectedExecutionHandler((runnable, exec) -> {
            log.warn("Excel export thread pool is full. Task rejected or throttled.");
            throw new IllegalStateException("Server is under heavy load. Please try your export request later.");
        });

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
