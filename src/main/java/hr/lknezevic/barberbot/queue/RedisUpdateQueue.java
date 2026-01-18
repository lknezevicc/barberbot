package hr.lknezevic.barberbot.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Primary
public class RedisUpdateQueue implements UpdateQueue {

    private static final String QUEUE_KEY = "telegram:updates";
    private static final String DLQ_KEY = QUEUE_KEY + ":dlq";
    private static final String PROCESSED_KEY_PREFIX = QUEUE_KEY + ":processed:";
    private static final Duration PROCESSED_TTL = Duration.ofHours(6);
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

    private final RedisTemplate<String, String> queueRedisTemplate;
    private final ObjectMapper queueObjectMapper;

    @Override
    @CircuitBreaker(name = "redisQueue")
    public Update dequeue() throws InterruptedException {
        String json = queueRedisTemplate.opsForList().rightPop(QUEUE_KEY, Duration.ofSeconds(3));
        if (json == null) return null;

        try {
            return queueObjectMapper.readValue(json, Update.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize Update from queue payload, moving to DLQ. payload={}", json, e);
            queueRedisTemplate.opsForList().leftPush(DLQ_KEY, json);
            queueRedisTemplate.opsForList().trim(DLQ_KEY, 0, DLQ_MAX_LEN - 1);
            return null;
        }
    }

    @Override
    @CircuitBreaker(name = "redisQueue")
    public void enqueue(Update update) {
        String processedKey = PROCESSED_KEY_PREFIX + update.getUpdateId();

        final String payload;
        try {
            payload = queueObjectMapper.writeValueAsString(update);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize updateId={}", update.getUpdateId(), e);
            return;
        }

        Long enqueued = queueRedisTemplate.execute(
                DEDUPE_AND_ENQUEUE,
                List.of(processedKey, QUEUE_KEY),
                String.valueOf(PROCESSED_TTL.toSeconds()),
                payload
        );

        if (enqueued == null || enqueued == 0L) {
            log.debug("UpdateId={} already seen (dedupe), skipping enqueue", update.getUpdateId());
        }
    }

    @Override
    @CircuitBreaker(name = "redisQueue")
    @Retry(name = "redisQueue")
    public void moveToDlq(Update update) {
        try {
            String payload = queueObjectMapper.writeValueAsString(update);
            queueRedisTemplate.opsForList().leftPush(DLQ_KEY, payload);
            queueRedisTemplate.opsForList().trim(DLQ_KEY, 0, DLQ_MAX_LEN - 1);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize update for DLQ. updateId={}", update.getUpdateId(), e);
        }
    }
}
