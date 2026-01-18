package hr.lknezevic.barberbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
class RedisConfiguration {

    @Bean
    RedisTemplate<String, String> queueRedisTemplate(RedisConnectionFactory connectionFactory) {
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
    ObjectMapper queueObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
