package hr.lknezevic.barberbot.bot.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";
    private static final int MAX_MESSAGES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String userId) {
        String key = KEY_PREFIX + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW.toMillis();

        try {
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            Long count = redisTemplate.opsForZSet().zCard(key);

            if (count != null && count >= MAX_MESSAGES) {
                log.warn("Rate limit exceeded for userId={}, count={}", userId, count);
                return false;
            }

            redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
            redisTemplate.expire(key, WINDOW.plusSeconds(5));
            return true;

        } catch (Exception e) {
            log.warn("Rate limiter check failed for userId={}, allowing through", userId, e);
            return true;
        }
    }
}
