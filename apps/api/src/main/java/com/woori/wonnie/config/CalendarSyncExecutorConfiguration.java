package com.woori.wonnie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class CalendarSyncExecutorConfiguration {
    @Bean
    Executor calendarSyncExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("calendar-sync-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}
