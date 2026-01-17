package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.queue.UpdateQueue;
import hr.lknezevic.barberbot.service.BotMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Primary
public final class RedisUpdateWorker implements Worker {

    private final UpdateQueue updateQueue;
    private final BotMessageService botMessageService;

    @Override
    public void process(Supplier<Boolean> runningSupplier) {
        while (runningSupplier.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Update update = updateQueue.dequeue();

                if (update == null || !update.hasMessage()) continue;

                log.debug("Processing updateId={}", update.getUpdateId());
                botMessageService.handleMessage(update.getMessage());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker stopped via interrupt");
                return;

            } catch (RedisSystemException | CancellationException e) {
                if (!runningSupplier.get()) {
                    log.info("Stopping worker because application is shutting down");
                    return;
                }
            } catch (Exception e) {
                log.error("Worker encountered an error", e);
            }
        }
    }
}
