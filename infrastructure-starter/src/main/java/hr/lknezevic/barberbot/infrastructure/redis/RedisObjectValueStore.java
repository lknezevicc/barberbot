package hr.lknezevic.barberbot.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.infrastructure.storage.ObjectValueStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisObjectValueStore<T> implements ObjectValueStore<T> {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Class<T> clazz;
    private final String keyPrefix;

    @Override
    @CircuitBreaker(name = "redisValue")
    @Retry(name = "redisValue")
    public void save(String key, T value, Duration ttl) {
        String redisKey = keyPrefix + key;
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(redisKey, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} for key={}", clazz.getName(), redisKey, e);
            throw new IllegalStateException("Serialization failed", e);
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public Optional<T> get(String key) {
        String redisKey = keyPrefix + key;

        String json;
        try {
            json = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Redis get failed for key={}", redisKey, e);
            return Optional.empty();
        }

        if (json == null) return Optional.empty();

        try {
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize {} for key={}, deleting corrupted entry", clazz.getName(), redisKey, e);
            delete(key);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public void delete(String key) {
        try {
            redisTemplate.delete(keyPrefix + key);
        } catch (Exception e) {
            log.warn("Redis delete failed for key={}", key, e);
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public boolean exists(String key) {
        String redisKey = keyPrefix + key;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            log.warn("Redis exists check failed for key={}", redisKey, e);
            return false;
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public void refreshTTL(String key, Duration ttl) {
        String redisKey = keyPrefix + key;
        try {
            Boolean success = redisTemplate.expire(redisKey, ttl);
            if (!Boolean.TRUE.equals(success)) {
                log.warn("Failed to refresh TTL for key={}", redisKey);
            }
        } catch (Exception e) {
            log.warn("Redis refreshTTL failed for key={}", redisKey, e);
        }
    }
}
