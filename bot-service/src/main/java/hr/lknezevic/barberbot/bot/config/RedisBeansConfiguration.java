package hr.lknezevic.barberbot.bot.config;

import hr.lknezevic.barberbot.infrastructure.redis.RedisFactory;
import hr.lknezevic.barberbot.infrastructure.storage.ObjectValueStore;
import hr.lknezevic.barberbot.infrastructure.storage.StringValueStore;
import hr.lknezevic.barberbot.model.dto.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RedisBeansConfiguration {

    @Bean
    ObjectValueStore<Session> sessionObjectValueStore(RedisFactory redisFactory) {
        return redisFactory.createObjectValueStore(Session.class, "telegram:sessions:");
    }

    @Bean
    StringValueStore userSessionStringValueStore(RedisFactory redisFactory) {
        return redisFactory.createStringValueStore("telegram:user:");
    }
}
