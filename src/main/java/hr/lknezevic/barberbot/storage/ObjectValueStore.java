package hr.lknezevic.barberbot.storage;

import java.time.Duration;
import java.util.Optional;

public interface ObjectValueStore<T> {
    void save(String key, T value, Duration ttl);
    Optional<T> get(String key);
    void delete(String key);
    boolean exists(String key);
    void refreshTTL(String key, Duration ttl);
}
