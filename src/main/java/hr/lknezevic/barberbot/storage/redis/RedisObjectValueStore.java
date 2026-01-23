package hr.lknezevic.barberbot.storage.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.storage.KeyValueStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisObjectValueStore<T> implements KeyValueStore<T> {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Class<T> clazz;

    @Override
    @CircuitBreaker(name = "redisValue")
    @Retry(name = "redisValue")
    public void save(String key, T value, Duration ttl) {
        final String redisKey = keyPrefix + key;
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(redisKey, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize value of type={} for key={}", clazz.getName(), redisKey, e);
            throw new IllegalStateException("Serialization failed", e);
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public Optional<T> get(String key) {
        final String redisKey = keyPrefix + key;

        final String json;
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
            log.error(
                    "Failed to deserialize value of type={} for key={}. Deleting corrupted entry.",
                    clazz.getName(), redisKey, e
            );
            delete(redisKey);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis delete failed for key={}", key, e);
        }
    }

    @Override
    @CircuitBreaker(name = "redisValue")
    public boolean exists(String key) {
        final String redisKey = keyPrefix + key;
        try {
            return redisTemplate.hasKey(redisKey);
        } catch (Exception e) {
            log.warn("Redis exists check failed for key={}", redisKey, e);
            return false;
        }
    }
}
