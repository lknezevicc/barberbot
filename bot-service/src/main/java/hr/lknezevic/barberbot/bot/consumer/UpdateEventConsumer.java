package hr.lknezevic.barberbot.bot.consumer;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateEventConsumer {

    private final UpdateEventParser updateEventParser;

    @RabbitListener(queues = RabbitQueues.UPDATE_INBOUND_QUEUE)
    public void onUpdateEvent(TelegramUpdateEvent event) {
        log.info("Received update event, tenant={}", event.tenantId());

        updateEventParser.parse(event).ifPresentOrElse(
                message -> log.info("Parsed InboundMessage: type={}, userId={}, chatId={}",
                        message.messageType(), message.userId(), message.chatId()),
                () -> log.warn("Could not parse update event, skipping")
        );
    }
}
