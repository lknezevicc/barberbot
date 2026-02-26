package hr.lknezevic.barberbot.bot.consumer;

import com.rabbitmq.client.Channel;
import hr.lknezevic.barberbot.bot.filter.RateLimiter;
import hr.lknezevic.barberbot.bot.filter.UserLock;
import hr.lknezevic.barberbot.bot.processor.MessageProcessor;
import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateEventConsumer {

    private static final String RATE_LIMIT_REPLY = "Polako! Previše poruka odjednom. Pričekaj malo.";

    private final UpdateEventParser updateEventParser;
    private final MessageProcessor messageProcessor;
    private final RateLimiter rateLimiter;
    private final UserLock userLock;
    private final ReplyPublisher replyPublisher;

    @RabbitListener(queues = RabbitQueues.UPDATE_INBOUND_QUEUE, ackMode = "MANUAL")
    public void onUpdateEvent(TelegramUpdateEvent event, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("Received update event, tenant={}", event.tenantId());

        var parsed = updateEventParser.parse(event);
        if (parsed.isEmpty()) {
            log.warn("Could not parse update event, skipping");
            channel.basicAck(deliveryTag, false);
            return;
        }

        InboundMessage message = parsed.get();

        if (!userLock.tryAcquire(message.userId())) {
            log.info("User {} is locked (message in flight), requeueing", message.userId());
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        try {
            if (!rateLimiter.isAllowed(message.userId())) {
                replyPublisher.publish(TelegramReplyEvent.of(message.chatId(), RATE_LIMIT_REPLY));
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("Processing message: type={}, userId={}, chatId={}",
                    message.messageType(), message.userId(), message.chatId());
            messageProcessor.process(message);
            channel.basicAck(deliveryTag, false);

        } finally {
            userLock.release(message.userId());
        }
    }
}
