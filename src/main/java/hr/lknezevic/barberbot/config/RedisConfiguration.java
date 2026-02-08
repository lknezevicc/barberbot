package hr.lknezevic.barberbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.storage.ObjectValueStore;
import hr.lknezevic.barberbot.storage.Queue;
import hr.lknezevic.barberbot.storage.StringSetStore;
import hr.lknezevic.barberbot.storage.StringValueStore;
import hr.lknezevic.barberbot.storage.redis.RedisFactory;
import hr.lknezevic.barberbot.storage.redis.RedisLock;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;

@Configuration
class RedisConfiguration {

    @Bean
    @Primary
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
    Queue<Update> updateRedisQueue(RedisFactory redisFactory) {
        return redisFactory.createQueue(
                Update.class,
                (u -> "telegram:updates:user:" + UpdateUtils.getUserId(u)),
                Duration.ofHours(6),
                (u -> u.getUpdateId().toString())
        );
    }

    @Bean
    ObjectValueStore<Session> sessionRedisKeyValueStore(RedisFactory redisFactory) {
        return redisFactory.createObjectValueStore(
                Session.class,
                "telegram:sessions:"
        );
    }

    @Bean
    StringValueStore userSessionRedisStringKeyValueStore(RedisFactory redisFactory) {
        return redisFactory.createStringValueStore("telegram:user:");
    }

    @Bean
    StringSetStore activeUserRedisStringSetStore(RedisFactory redisFactory) {
        return redisFactory.createStringSetStore("telegram:active-users");
    }

    @Bean
    RedisLock redisLock(RedisTemplate<String, String> redisTemplate) {
        return new RedisLock(redisTemplate);
    }
}
