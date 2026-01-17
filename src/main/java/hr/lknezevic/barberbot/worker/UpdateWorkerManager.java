package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.config.application.WorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class UpdateWorkerManager implements SmartLifecycle {

    private final WorkerConfig workerConfig;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ObjectProvider<Worker> workerProvider;
    private volatile boolean running = false;

    public UpdateWorkerManager(
            WorkerConfig workerConfig,
            @Qualifier("updateTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
            ObjectProvider<Worker> workerProvider) {
        this.workerConfig = workerConfig;
        this.taskExecutor = taskExecutor;
        this.workerProvider = workerProvider;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void start() {
        log.info("Starting {} update workers", workerConfig.count());
        running = true;

        for (int i = 0; i < workerConfig.count(); i++) {
            taskExecutor.execute(() -> {
                while (!Thread.currentThread().isInterrupted() && running) {
                    Worker worker = workerProvider.getObject();
                    try {
                        worker.process(() -> running);
                    } catch (Exception e) {
                        log.error("Worker crashed, restarting...", e);
                    }
                }
            });
        }

        log.info("All workers started");
    }

    @Override
    public void stop() {
        log.info("Stopping update workers");
        running = false;
        taskExecutor.getThreadPoolExecutor().shutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
