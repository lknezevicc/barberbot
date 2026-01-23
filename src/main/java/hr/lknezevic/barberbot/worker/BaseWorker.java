package hr.lknezevic.barberbot.worker;

import hr.lknezevic.barberbot.signal.ShutdownSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseWorker<T> implements Worker {

    protected static final int DEFAULT_BACKOFF_MS = 2000;
    protected final ShutdownSignal shutdownSignal;

    protected void backoff(int backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    protected final void run(
            Supplier<Boolean> runningSupplier,
            Supplier<T> itemSupplier,
            Consumer<T> itemConsumer,
            int backoffMs) {
        while (runningSupplier.get() && !Thread.currentThread().isInterrupted()) {
            final T item;
            try {
                item = itemSupplier.get();
                if (item == null) continue;
            } catch (Exception e) {
                if (shutdownSignal.isShuttingDown()) return;
                log.warn("Worker error, backing off {}ms", backoffMs, e);
                backoff(backoffMs);
                continue;
            }

            try {
                itemConsumer.accept(item);
            } catch (Exception e) {
                log.error("Processing failed: {}", e.getMessage(), e);
            }
        }
    }
}
