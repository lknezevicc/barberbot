package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.queue.UpdateQueue;
import hr.lknezevic.barberbot.service.BotMessageService;
import hr.lknezevic.barberbot.signal.ShutdownSignal;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
abstract class BaseWorker implements Worker {
    protected static final int THREAD_BACKOFF_MS = 2000;

    protected final UpdateQueue updateQueue;
    protected final BotMessageService botMessageService;
    protected final ShutdownSignal shutdownSignal;

    protected void backoff() {
        try {
            Thread.sleep(THREAD_BACKOFF_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
