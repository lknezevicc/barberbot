package hr.lknezevic.barberbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.storage.KeyValueStore;
import hr.lknezevic.barberbot.storage.Queue;
import hr.lknezevic.barberbot.storage.StringKeyValueStore;
import hr.lknezevic.barberbot.storage.redis.RedisObjectValueStore;
import hr.lknezevic.barberbot.storage.redis.RedisQueue;
import hr.lknezevic.barberbot.storage.redis.RedisStringValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;

@Configuration
class RedisConfiguration {

    @Bean
    RedisTemplate<String, String> defaultRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer string = new StringRedisSerializer();
        template.setKeySerializer(string);
        template.setValueSerializer(string);
        template.setHashKeySerializer(string);
        template.setHashValueSerializer(string);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    ObjectMapper defaultObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    Queue<Update> updateRedisQueue(
            RedisTemplate<String, String> defaultRedisTemplate,
            ObjectMapper defaultObjectMapper
    ) {
        String queueKey = "telegram:updates";
        return new RedisQueue<>(
                defaultRedisTemplate,
                defaultObjectMapper,
                Update.class,
                queueKey,
                queueKey + ":dlq",
                queueKey + ":processed",
                Duration.ofHours(6),
                (u -> u.getUpdateId().toString())
        );
    }

    @Bean
    KeyValueStore<Session> sessionRedisKeyValueStore(
            RedisTemplate<String, String> defaultRedisTemplate,
            ObjectMapper defaultObjectMapper
    ) {
        return new RedisObjectValueStore<>(
                defaultRedisTemplate,
                defaultObjectMapper,
                "telegram:sessions:",
                Session.class
        );
    }

    @Bean
    StringKeyValueStore userSessionRedisStringKeyValueStore(
            RedisTemplate<String, String> defaultRedisTemplate
    ) {
        return new RedisStringValueStore(
                defaultRedisTemplate,
                "telegram:user:"
        );
    }
}
