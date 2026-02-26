package hr.lknezevic.barberbot.bot.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingMessageBuffer {

    private static final String KEY_PREFIX = "pending:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void stash(InboundMessage message) {
        String key = KEY_PREFIX + message.userId();
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, TTL);
            log.debug("Stashed pending message for userId={}, type={}", message.userId(), message.messageType());
        } catch (JsonProcessingException e) {
            log.error("Failed to stash pending message for userId={}", message.userId(), e);
        }
    }

    public List<InboundMessage> drainAll(String userId) {
        String key = KEY_PREFIX + userId;
        try {
            List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
            redisTemplate.delete(key);

            if (raw == null || raw.isEmpty()) {
                return Collections.emptyList();
            }

            return raw.stream()
                    .map(this::deserialize)
                    .filter(m -> m != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to drain pending messages for userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    private InboundMessage deserialize(String json) {
        try {
            return objectMapper.readValue(json, InboundMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize pending message, skipping", e);
            return null;
        }
    }
}
