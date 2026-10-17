package hr.lknezevic.barberbot.bot.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ZSetOperations<String, String> zSetOps;

    @InjectMocks
    private RateLimiter rateLimiter;

    @Test
    void isAllowed_underLimit_returnsTrue() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard("ratelimit:user-1")).thenReturn(5L);

        boolean result = rateLimiter.isAllowed("user-1");

        assertThat(result).isTrue();
    }

    @Test
    void isAllowed_atLimit_returnsFalse() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard("ratelimit:user-1")).thenReturn(10L);

        boolean result = rateLimiter.isAllowed("user-1");

        assertThat(result).isFalse();
    }

    @Test
    void isAllowed_overLimit_returnsFalse() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard("ratelimit:user-1")).thenReturn(15L);

        boolean result = rateLimiter.isAllowed("user-1");

        assertThat(result).isFalse();
    }

    @Test
    void isAllowed_redisException_allowsThrough() {
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("Redis connection failed"));

        boolean result = rateLimiter.isAllowed("user-1");

        assertThat(result).isTrue();
    }

    @Test
    void isAllowed_nullCount_allowsThrough() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(null);

        boolean result = rateLimiter.isAllowed("user-1");

        assertThat(result).isTrue();
    }
}
