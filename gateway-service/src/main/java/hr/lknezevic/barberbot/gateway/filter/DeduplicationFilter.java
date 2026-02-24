package hr.lknezevic.barberbot.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DeduplicationFilter {

    private static final long TTL_SECONDS = 60;

    private final ConcurrentHashMap<Long, Instant> seenUpdateIds = new ConcurrentHashMap<>();

    public boolean isDuplicate(long updateId) {
        Instant previous = seenUpdateIds.putIfAbsent(updateId, Instant.now());
        if (previous != null) {
            log.debug("Duplicate update_id={} detected, skipping", updateId);
            return true;
        }
        return false;
    }

    @Scheduled(fixedRate = 30_000)
    void evictExpired() {
        Instant cutoff = Instant.now().minusSeconds(TTL_SECONDS);
        seenUpdateIds.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}
