package hr.lknezevic.barberbot.storage.redis;

import hr.lknezevic.barberbot.storage.StringKeyValueStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
public class RedisStringValueStore implements StringKeyValueStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final String keyPrefix;

    @Override
    @CircuitBreaker(name = "redisValue")
    public void save(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(keyPrefix + key, value, ttl);
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public Optional<String> get(String key) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(keyPrefix + key)
        );
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public void delete(String key) {
        redisTemplate.delete(keyPrefix + key);
    }
}
