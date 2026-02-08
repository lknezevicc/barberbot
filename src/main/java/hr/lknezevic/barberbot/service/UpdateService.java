package hr.lknezevic.barberbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.model.LlmResult;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public final class UpdateService {

    private static final String ACTIVE_USERS_SET_KEY = "telegram:active-users";
    private static final String USER_QUEUE_KEY_PREFIX = "telegram:updates:user:";
    private static final DefaultRedisScript<Long> ENQUEUE_SCRIPT = new DefaultRedisScript<>(
            """
            -- KEYS[1] = active users set key
            -- KEYS[2] = user queue key
            -- ARGV[1] = userId
            -- ARGV[2] = payload (json)
            redis.call("SADD", KEYS[1], ARGV[1])
            redis.call("LPUSH", KEYS[2], ARGV[2])
            return 1
            """,
            Long.class
    );

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;

    public UpdateHandlerContext getContext(Update update) {
        String userId = UpdateUtils.getUserId(update);
        return sessionService.getSessionByUserId(userId)
                .map(s -> new UpdateHandlerContext(update, s, null))
                .orElseGet(() -> new UpdateHandlerContext(update, sessionService.startSession(userId), null));
    }

    public UpdateHandlerContext mergeWithLlmResult(
            UpdateHandlerContext updateHandlerContext,
            LlmResult llmResult
    ) {
        return updateHandlerContext.withSession(
                updateHandlerContext.session().withBookingDraft(
                        updateHandlerContext.session().bookingDraft().merge(llmResult.bookingDraft())
                )
        );
    }

    public void enqueue(Update update) {
        final String userId = UpdateUtils.getUserId(update);
        log.info("enqueue {}", userId);
        if (userId == null) return;

        final String json;
        try {
            json = objectMapper.writeValueAsString(update);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize value of type={}", update.getClass().getName(), e);
            return;
        }

        String queueKey = USER_QUEUE_KEY_PREFIX + userId;

        var result = redisTemplate.execute(ENQUEUE_SCRIPT, List.of(ACTIVE_USERS_SET_KEY, queueKey), userId, json);
        log.info("enqueue {}", result);
    }
}
