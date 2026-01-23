package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.handler.HandlerPipeline;
import hr.lknezevic.barberbot.signal.ShutdownSignal;
import hr.lknezevic.barberbot.storage.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
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

    private final Queue<Update> redisQueue;
    private final HandlerPipeline<Update> updateHandlerPipeline;
    private final Supplier<Update> itemSupplier = this::fetchUpdate;
    private final Consumer<Update> itemConsumer = this::processUpdate;

    public RedisUpdateWorker(
            ShutdownSignal shutdownSignal,
            Queue<Update> redisQueue,
            HandlerPipeline<Update> updateHandlerPipeline) {
        super(shutdownSignal);
        this.redisQueue = redisQueue;
        this.updateHandlerPipeline = updateHandlerPipeline;
    }

    @Override
    public void process(Supplier<Boolean> runningSupplier) {
        run(runningSupplier, itemSupplier, itemConsumer, DEFAULT_BACKOFF_MS);
    }

    private Update fetchUpdate() {
        try {
            return redisQueue.dequeue(Duration.ofSeconds(3)).orElse(null);
        } catch (Exception e) {
            if (shutdownSignal.isShuttingDown()) return null;
            log.warn("Error fetching update from queue, backing off {}ms", DEFAULT_BACKOFF_MS, e);
            backoff(DEFAULT_BACKOFF_MS);
            return null;
        }
    }

    private void processUpdate(Update update) {
        try {
            updateHandlerPipeline.process(update);
        } catch (Exception e) {
            log.error("Handler failed for updateId={}, moving to DLQ", update.getUpdateId(), e);
            try {
                redisQueue.moveToDlq(update);
            } catch (Exception ex) {
                log.error("Failed to move updateId={} to DLQ", update.getUpdateId(), ex);
            }
        }
    }
}
