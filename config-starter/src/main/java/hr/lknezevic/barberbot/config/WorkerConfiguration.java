package hr.lknezevic.barberbot.config;

import hr.lknezevic.barberbot.config.application.WorkerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
class WorkerConfiguration {

    @Bean(name = "updateTaskExecutor")
    ThreadPoolTaskExecutor updateTaskExecutor(WorkerConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.count());
        executor.setMaxPoolSize(config.count());
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix(config.threadName());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    @Bean
    TaskExecutor virtualThreadTaskExecutor() {
        return new VirtualThreadTaskExecutor();
    }
}
