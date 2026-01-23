package hr.lknezevic.barberbot.storage;

import java.time.Duration;
import java.util.Optional;

public interface StringKeyValueStore {
    void save(String key, String value, Duration ttl);
    Optional<String> get(String key);
    void delete(String key);
}
