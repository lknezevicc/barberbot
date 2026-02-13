package hr.lknezevic.barberbot.bot.publisher;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(TelegramReplyEvent event) {
        log.debug("Publishing reply event for chatId={}", event.chatId());
        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.REPLY_OUTBOUND_ROUTING_KEY,
                event
        );
    }
}
