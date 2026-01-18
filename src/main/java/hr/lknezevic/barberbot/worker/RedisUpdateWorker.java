package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.queue.UpdateQueue;
import hr.lknezevic.barberbot.service.BotMessageService;
import hr.lknezevic.barberbot.signal.ShutdownSignal;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Primary
final class RedisUpdateWorker extends BaseWorker {

    public RedisUpdateWorker(
            UpdateQueue updateQueue,
            BotMessageService botMessageService,
            ShutdownSignal shutdownSignal) {
        super(updateQueue, botMessageService, shutdownSignal);
    }

    @Override
    public void process(Supplier<Boolean> runningSupplier) {
        while (runningSupplier.get() && !Thread.currentThread().isInterrupted()) {
            final Update update;

            try {
                update = updateQueue.dequeue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker stopped via interrupt");
                return;
            } catch (CallNotPermittedException e) {
                if (shutdownSignal.isShuttingDown()) return;
                log.warn("Redis circuit breaker OPEN, backing off {}ms", THREAD_BACKOFF_MS);
                backoff();
                continue;
            } catch (DataAccessException | CancellationException e) {
                if (shutdownSignal.isShuttingDown()) return;
                log.warn("Redis unavailable during dequeue, backing off {}ms: {}", THREAD_BACKOFF_MS, e.toString());
                backoff();
                continue;
            } catch (Exception e) {
                if (shutdownSignal.isShuttingDown()) return;
                log.error("Unexpected dequeue failure, backing off {}ms", THREAD_BACKOFF_MS, e);
                backoff();
                continue;
            }

            if (update == null || !update.hasMessage()) continue;
            try {
                log.debug("Processing updateId={}", update.getUpdateId());
                botMessageService.handleMessage(update.getMessage());
            } catch (Exception e) {
                log.error("Processing failed for updateId={}, moving to DLQ: {}",
                        update.getUpdateId(), e.getMessage(), e);
                try {
                    updateQueue.moveToDlq(update);
                } catch (Exception ex) {
                    log.error("Failed to move updateId={} to DLQ: {}",
                            update.getUpdateId(), ex.getMessage(), ex);
                }
            }
        }
    }
}
