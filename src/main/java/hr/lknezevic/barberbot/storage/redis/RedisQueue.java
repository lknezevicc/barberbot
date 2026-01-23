package hr.lknezevic.barberbot.storage.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.storage.Queue;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class RedisQueue<T> implements Queue<T> {

    private static final int DLQ_MAX_LEN = 1000;
    private static final DefaultRedisScript<Long> DEDUPE_AND_ENQUEUE = new DefaultRedisScript<>(
            """
            -- KEYS[1] processedKey
            -- KEYS[2] queueKey
            -- ARGV[1] ttlSeconds
            -- ARGV[2] payload
            if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1]) then
              redis.call('LPUSH', KEYS[2], ARGV[2])
              return 1
            else
              return 0
            end
            """,
            Long.class
    );

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Class<T> clazz;
    private final String queueKey;
    private final String dlqKey;
    private final String processedKey;
    private final Duration processedTtl;
    private final Function<T, String> idExtractor;

    @Override
    @CircuitBreaker(name = "redisQueue")
    public void enqueue(T value) {
        String processedKeyWithId = processedKey + ":" + idExtractor.apply(value);

        final String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} with ID={}", clazz.getName(), idExtractor.apply(value), e);
            moveToDlq(value);
            return;
        }

        Long enqueued = redisTemplate.execute(
                DEDUPE_AND_ENQUEUE,
                List.of(processedKeyWithId, queueKey),
                String.valueOf(processedTtl.toSeconds()),
                json
        );

        if (enqueued == 0L) {
            log.debug("{} with ID={} already seen (dedupe), skipping enqueue", clazz.getName(), idExtractor.apply(value));
        }
    }

    @Override
    @CircuitBreaker(name = "redisQueue")
    public Optional<T> dequeue(Duration timeout) {
        String json = redisTemplate.opsForList().rightPop(queueKey, timeout);
        if (json == null) return Optional.empty();

        try {
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize {} from queue payload, moving to DLQ. payload={}", clazz.getName(), json, e);
            redisTemplate.opsForList().leftPush(dlqKey, json);
            redisTemplate.opsForList().trim(dlqKey, 0, DLQ_MAX_LEN - 1);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "redisQueue")
    public void moveToDlq(T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForList().leftPush(dlqKey, json);
            redisTemplate.opsForList().trim(dlqKey, 0, DLQ_MAX_LEN - 1);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} for DLQ (queueKey={})", clazz.getName(), dlqKey, e);
        }
    }
}
