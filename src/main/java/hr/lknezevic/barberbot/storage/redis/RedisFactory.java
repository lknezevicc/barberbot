package hr.lknezevic.barberbot.storage.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.storage.ObjectValueStore;
import hr.lknezevic.barberbot.storage.Queue;
import hr.lknezevic.barberbot.storage.StringSetStore;
import hr.lknezevic.barberbot.storage.StringValueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public final class RedisFactory {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Queue<T> createQueue(
            Class<T> clazz,
            Function<T, String> queueKeyResolver,
            Duration processedTtl,
            Function<T, String> idExtractor
    ) {
        return new RedisQueue<>(
                redisTemplate,
                objectMapper,
                clazz,
                queueKeyResolver,
                processedTtl,
                idExtractor
        );
    }

    public <T> ObjectValueStore<T> createObjectValueStore(
        Class<T> clazz,
        String keyPrefix
    ) {
        return new RedisObjectValueStore<>(
                redisTemplate,
                objectMapper,
                clazz,
                keyPrefix
        );
    }

    public StringValueStore createStringValueStore(String keyPrefix) {
        return new RedisStringValueStore(redisTemplate, keyPrefix);
    }

    public StringSetStore createStringSetStore(String key) {
        return new RedisStringSetStore(redisTemplate, key);
    }
}
