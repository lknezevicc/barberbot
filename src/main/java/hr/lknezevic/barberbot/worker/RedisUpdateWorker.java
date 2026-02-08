package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.handler.HandlerPipeline;
import hr.lknezevic.barberbot.model.LockedUpdate;
import hr.lknezevic.barberbot.signal.ShutdownSignal;
import hr.lknezevic.barberbot.storage.Queue;
import hr.lknezevic.barberbot.storage.StringSetStore;
import hr.lknezevic.barberbot.storage.redis.RedisLock;
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
public final class RedisUpdateWorker extends BaseWorker<LockedUpdate> {

    private final static String TELEGRAM_UPDATE_QUEUE_KEY = "telegram:updates:user:";
    private final static long REDIS_LOCK_TTL_SECONDS = 30;

    private final TaskExecutor taskExecutor;
    private final RedisLock redisLock;
    private final Queue<Update> redisQueue;
    private final StringSetStore activeUserSetStore;
    private final HandlerPipeline<Update> updateHandlerPipeline;
    private final Supplier<LockedUpdate> itemSupplier = this::fetchUpdate;
    private final Consumer<LockedUpdate> itemConsumer = this::processUpdate;

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

    private LockedUpdate fetchUpdate() {
        final String userId = activeUserSetStore.pop().orElse(null);
        if  (userId == null) return null;

        final String token;
        try {
            token = redisLock.acquire(userId, REDIS_LOCK_TTL_SECONDS);
        } catch (Exception e) {
            if (shutdownSignal.isShuttingDown()) return null;
            log.warn("Error fetching userId from active-users list, backing off {}ms", DEFAULT_BACKOFF_MS, e);
            backoff(DEFAULT_BACKOFF_MS);
            return null;
        }

        if (token == null) {
            activeUserSetStore.add(userId);
            backoff(50);
            return null;
        }

        try {
            Update update = redisQueue
                    .dequeue(TELEGRAM_UPDATE_QUEUE_KEY + userId, Duration.ofSeconds(3))
                    .orElse(null);
            if (update == null) {
                redisLock.release(userId, token);
                return null;
            }

            return new LockedUpdate(update, token, userId);
        } catch (Exception e) {
            log.info("SHUTDOWN SIGNAL: " + shutdownSignal.isShuttingDown());
            if (shutdownSignal.isShuttingDown()) return null;
            log.warn("Error fetching update from queue, backing off {}ms", DEFAULT_BACKOFF_MS, e);
            backoff(DEFAULT_BACKOFF_MS);
            return null;
        }
    }

    private void processUpdate(LockedUpdate lockedUpdate) {
        final Update update = lockedUpdate.update();
        taskExecutor.execute(() -> {
            try {
                log.info("Processing update {} in thread {}", update.getUpdateId(), Thread.currentThread().getName());
                updateHandlerPipeline.process(update);
            } catch (Exception e) {
                log.error("Handler failed for updateId={}, moving to DLQ", update.getUpdateId(), e);
                try {
                    redisQueue.moveToDlq(update);
                } catch (Exception dlqEx) {
                    log.error("Failed to move update to DLQ", dlqEx);
                }
            } finally {
                redisLock.release(lockedUpdate.userId(), lockedUpdate.token());

                if (redisQueue.size(TELEGRAM_UPDATE_QUEUE_KEY + lockedUpdate.userId()) > 0) {
                    activeUserSetStore.add(lockedUpdate.userId());
                }
            }
        });
    }
}
