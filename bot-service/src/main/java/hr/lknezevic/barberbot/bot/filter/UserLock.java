package hr.lknezevic.barberbot.bot.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLock {

    private static final String KEY_PREFIX = "lock:user:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;

    public boolean tryAcquire(String userId) {
        String key = KEY_PREFIX + userId;
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", LOCK_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Failed to acquire user lock for userId={}, allowing through", userId, e);
            return true;
        }
    }

    public void release(String userId) {
        String key = KEY_PREFIX + userId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to release user lock for userId={}", userId, e);
        }
    }
}
