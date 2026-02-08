package hr.lknezevic.barberbot.storage.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class RedisLock {

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call("GET", KEYS[1]) == ARGV[1] then
                return redis.call("DEL", KEYS[1])
            else
                return 0
            end
            """,
            Long.class
    );

    private final RedisTemplate<String, String> redisTemplate;

    public String acquire(String userId, long ttlSeconds) {
        String lockKey = "telegram:updates:user:" + userId + ":lock";
        String token = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, token, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success) ? token : null;
    }

    public boolean release(String userId, String token) {
        log.info("[RELEASE] userId: {}, token: {}", userId, token);
        String lockKey = "telegram:updates:user:" + userId + ":lock";
        Long result = redisTemplate.execute(RELEASE_LOCK_SCRIPT, Arrays.asList(lockKey, token));
        return Long.valueOf(1).equals(result);
    }
}
