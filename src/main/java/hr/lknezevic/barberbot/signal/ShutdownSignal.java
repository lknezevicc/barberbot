package hr.lknezevic.barberbot.signal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public final class ShutdownSignal {
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @EventListener(ContextClosedEvent.class)
    void onContextClosed() {
        log.info("Shutdown signal received - entering graceful shutdown");
        shuttingDown.set(true);
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
