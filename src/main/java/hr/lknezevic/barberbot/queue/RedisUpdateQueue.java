package hr.lknezevic.barberbot.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@Primary
public final class RedisUpdateQueue implements UpdateQueue {

    private static final String QUEUE_KEY = "telegram:updates";
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Update dequeue() throws InterruptedException {
        return (Update) redisTemplate
                .opsForList()
                .rightPop(QUEUE_KEY, Duration.ofSeconds(3));
    }

    @Override
    public void enqueue(Update update) {
        redisTemplate.opsForList().leftPush(QUEUE_KEY, update);
    }
}
