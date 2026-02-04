package hr.lknezevic.barberbot.storage.redis;

import hr.lknezevic.barberbot.storage.StringValueStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisStringValueStore implements StringValueStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final String keyPrefix;

    @Override
    @Retry(name = "redisValue")
    @CircuitBreaker(name = "redisValue")
    public void save(String key, String value, Duration ttl) {
        final String redisKey = keyPrefix + key;
        try {
            redisTemplate.opsForValue().set(redisKey, value, ttl);
        } catch (Exception e) {
            log.error("Redis save failed for key={}", redisKey, e);
            throw new IllegalStateException("Redis save failed", e);
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public Optional<String> get(String key) {
        final String redisKey = keyPrefix + key;
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey));
        } catch (Exception e) {
            log.warn("Redis get failed for key={}", redisKey, e);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public void delete(String key) {
        final String redisKey = keyPrefix + key;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis delete failed for key={}", redisKey, e);
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public void refreshTTL(String key, Duration ttl) {
        final String redisKey = keyPrefix + key;
        try {
            Boolean success = redisTemplate.expire(redisKey, ttl);
            if (success == null || !success) {
                log.warn("Redis TTL refresh failed for key={}", redisKey);
            }
        } catch (Exception e) {
            log.warn("Redis refreshTTL failed for key={}", redisKey, e);
        }
    }
}
