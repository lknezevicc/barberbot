package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.config.WorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class UpdateWorkerManager implements SmartLifecycle {

    private final WorkerConfig workerConfig;
    private final TaskExecutor taskExecutor;
    private final ApplicationContext applicationContext;
    private volatile boolean running = false;

    public UpdateWorkerManager(
            WorkerConfig workerConfig,
            @Qualifier("updateTaskExecutor") TaskExecutor taskExecutor,
            ApplicationContext applicationContext) {
        this.workerConfig = workerConfig;
        this.taskExecutor = taskExecutor;
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void start() {
        log.info("Starting {} update workers", workerConfig.count());

        for (int i = 0; i < workerConfig.count(); i++) {
            UpdateWorker worker = applicationContext.getBean(UpdateWorker.class);
            taskExecutor.execute(worker::process);
        }

        running = true;
    }

    @Override
    public void stop() {
        log.info("Stopping update workers");

        ((ThreadPoolTaskExecutor) taskExecutor).shutdown();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
