package hr.lknezevic.barberbot.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.infrastructure.storage.ObjectValueStore;
import hr.lknezevic.barberbot.infrastructure.storage.StringValueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class RedisFactory {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> ObjectValueStore<T> createObjectValueStore(Class<T> clazz, String keyPrefix) {
        return new RedisObjectValueStore<>(redisTemplate, objectMapper, clazz, keyPrefix);
    }

    public StringValueStore createStringValueStore(String keyPrefix) {
        return new RedisStringValueStore(redisTemplate, keyPrefix);
    }
}
