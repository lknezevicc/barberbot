package hr.lknezevic.barberbot.storage.redis;

import hr.lknezevic.barberbot.storage.StringSetStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisStringSetStore implements StringSetStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final String key;

    @Override
    @Retry(name = "redisValue")
    @CircuitBreaker(name = "redisValue")
    public void add(String value) {
        try {
            redisTemplate.opsForSet().add(key, value);
        } catch (Exception e) {
            log.error("Redis save failed for key={}", key, e);
            throw new IllegalStateException("Redis save failed", e);
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public Optional<String> pop() {
        try {
            return Optional.ofNullable(redisTemplate.opsForSet().pop(key));
        } catch (Exception e) {
            log.warn("Redis pop failed for key={}", key, e);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public void remove(String value) {
        try {
            redisTemplate.opsForSet().remove(key, value);
        } catch (Exception e) {
            log.warn("Redis remove failed for key={}", key, e);
        }
    }
}
