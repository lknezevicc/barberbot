package hr.lknezevic.barberbot.bot.consumer;

import hr.lknezevic.barberbot.bot.filter.RateLimiter;
import hr.lknezevic.barberbot.bot.processor.MessageProcessor;
import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateEventConsumer {

    private static final String RATE_LIMIT_REPLY = "Polako! Previše poruka odjednom. Pričekaj malo.";

    private final UpdateEventParser updateEventParser;
    private final MessageProcessor messageProcessor;
    private final RateLimiter rateLimiter;
    private final ReplyPublisher replyPublisher;

    @RabbitListener(queues = RabbitQueues.UPDATE_INBOUND_QUEUE)
    public void onUpdateEvent(TelegramUpdateEvent event) {
        log.info("Received update event, tenant={}", event.tenantId());

        updateEventParser.parse(event).ifPresentOrElse(
                message -> {
                    if (!rateLimiter.isAllowed(message.userId())) {
                        replyPublisher.publish(TelegramReplyEvent.of(
                                message.chatId(), RATE_LIMIT_REPLY));
                        return;
                    }

                    log.info("Processing message: type={}, userId={}, chatId={}",
                            message.messageType(), message.userId(), message.chatId());
                    messageProcessor.process(message);
                },
                () -> log.warn("Could not parse update event, skipping")
        );
    }
}
