package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.storage.Queue;
import hr.lknezevic.barberbot.service.BotMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class InMemoryUpdateWorker implements Worker {

    private final Queue queue;
    private final BotMessageService botMessageService;

    @Override
    public void process(Supplier<Boolean> runningSupplier) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Update update = queue.dequeue();
                log.debug("Processing updateId={}", update.getUpdateId());

                botMessageService.handleMessage(update.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Worker stopped");
        } catch (Exception e) {
            log.error("Worker crashed", e);
        }
    }
}
