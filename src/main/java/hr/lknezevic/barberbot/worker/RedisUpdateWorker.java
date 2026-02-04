package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.handler.HandlerPipeline;
import hr.lknezevic.barberbot.signal.ShutdownSignal;
import hr.lknezevic.barberbot.storage.Queue;
import hr.lknezevic.barberbot.storage.StringSetStore;
import hr.lknezevic.barberbot.storage.redis.RedisLock;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Primary
public final class RedisUpdateWorker extends BaseWorker<Update> {

    private final static String TELEGRAM_UPDATE_QUEUE_KEY = "telegram:updates:user:";
    private final static long REDIS_LOCK_TTL_SECONDS = 8;

    private final TaskExecutor taskExecutor;
    private final RedisLock redisLock;
    private final Queue<Update> redisQueue;
    private final StringSetStore activeUserSetStore;
    private final HandlerPipeline<Update> updateHandlerPipeline;
    private final Supplier<Update> itemSupplier = this::fetchUpdate;
    private final Consumer<Update> itemConsumer = this::processUpdate;

    public RedisUpdateWorker(
            ShutdownSignal shutdownSignal,
            @Qualifier("virtualThreadTaskExecutor") TaskExecutor taskExecutor,
            RedisLock redisLock,
            Queue<Update> redisQueue, StringSetStore activeUserSetStore,
            HandlerPipeline<Update> updateHandlerPipeline) {
        super(shutdownSignal);
        this.taskExecutor = taskExecutor;
        this.redisLock = redisLock;
        this.redisQueue = redisQueue;
        this.activeUserSetStore = activeUserSetStore;
        this.updateHandlerPipeline = updateHandlerPipeline;
    }

    @Override
    public void process(Supplier<Boolean> runningSupplier) {
        run(runningSupplier, itemSupplier, itemConsumer, DEFAULT_BACKOFF_MS);
    }

    private Update fetchUpdate() {
        String userId = activeUserSetStore.pop().orElse(null);
        if  (userId == null) return null;

        try {
            return redisQueue.dequeue(TELEGRAM_UPDATE_QUEUE_KEY + userId, Duration.ofSeconds(3)).orElse(null);
        } catch (Exception e) {
            log.info("SHUTDOWN SIGNAL: " + shutdownSignal.isShuttingDown());
            if (shutdownSignal.isShuttingDown()) return null;
            log.warn("Error fetching update from queue, backing off {}ms", DEFAULT_BACKOFF_MS, e);
            backoff(DEFAULT_BACKOFF_MS);
            return null;
        }
    }

    private void processUpdate(Update update) {
        final String userId = UpdateUtils.getUserId(update);
        if (userId == null) return;

        final String token = redisLock.acquire(userId, REDIS_LOCK_TTL_SECONDS);
        if (token == null) return;

        taskExecutor.execute(() -> {
            try {
                log.info("Processing update {} in thread {}", update.getUpdateId(), Thread.currentThread().getName());
                updateHandlerPipeline.process(update);
            } catch (Exception e) {
                log.error("Handler failed for updateId={}, moving to DLQ", update.getUpdateId(), e);
                redisQueue.moveToDlq(update);
            } finally {
                redisLock.release(userId, token);
            }
        });
    }

}
