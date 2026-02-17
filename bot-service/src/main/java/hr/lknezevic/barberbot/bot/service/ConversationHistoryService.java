package hr.lknezevic.barberbot.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryService {

    private static final String KEY_PREFIX = "conv:";
    private static final int MAX_ENTRIES = 20;
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "redisValue", fallbackMethod = "emptyHistoryFallback")
    public List<ConversationEntry> getHistory(String userId) {
        String key = KEY_PREFIX + userId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return raw.stream()
                .map(this::deserialize)
                .filter(e -> e != null)
                .toList();
    }

    @CircuitBreaker(name = "redisValue")
    public void append(String userId, ConversationEntry entry) {
        String key = KEY_PREFIX + userId;
        String json = serialize(entry);
        if (json == null) return;

        redisTemplate.opsForList().rightPush(key, json);
        redisTemplate.opsForList().trim(key, -MAX_ENTRIES, -1);
        redisTemplate.expire(key, TTL);
    }

    @CircuitBreaker(name = "redisValue")
    public void clear(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    @SuppressWarnings("unused")
    private List<ConversationEntry> emptyHistoryFallback(String userId, Throwable t) {
        log.warn("Conversation history unavailable for userId={}, returning empty", userId, t);
        return Collections.emptyList();
    }

    private String serialize(ConversationEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ConversationEntry", e);
            return null;
        }
    }

    private ConversationEntry deserialize(String json) {
        try {
            return objectMapper.readValue(json, ConversationEntry.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize ConversationEntry, skipping", e);
            return null;
        }
    }
}
