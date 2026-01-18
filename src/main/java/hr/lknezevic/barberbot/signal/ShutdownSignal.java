package hr.lknezevic.barberbot.signal;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public final class ShutdownSignal {
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @PreDestroy
    void onShutdown() {
        shuttingDown.set(true);
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
