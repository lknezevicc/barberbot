package hr.lknezevic.barberbot.bot.consumer;

import com.rabbitmq.client.Channel;
import hr.lknezevic.barberbot.bot.filter.PendingMessageBuffer;
import hr.lknezevic.barberbot.bot.filter.RateLimiter;
import hr.lknezevic.barberbot.bot.filter.UserLock;
import hr.lknezevic.barberbot.bot.processor.MessageProcessor;
import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.enums.MessageType;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateEventConsumer {

    private static final String RATE_LIMIT_REPLY = "Polako! Previše poruka odjednom. Pričekaj malo.";

    private final UpdateEventParser updateEventParser;
    private final MessageProcessor messageProcessor;
    private final RateLimiter rateLimiter;
    private final UserLock userLock;
    private final PendingMessageBuffer pendingMessageBuffer;
    private final ReplyPublisher replyPublisher;

    @RabbitListener(queues = RabbitQueues.UPDATE_INBOUND_QUEUE, ackMode = "MANUAL")
    public void onUpdateEvent(TelegramUpdateEvent event, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (event.correlationId() != null) {
            MDC.put("correlationId", event.correlationId());
        }
        if (event.tenantId() != null) {
            MDC.put("tenantId", event.tenantId());
        }

        log.info("Received update event, tenant={}, correlationId={}", event.tenantId(), event.correlationId());

        var parsed = updateEventParser.parse(event);
        if (parsed.isEmpty()) {
            log.warn("Could not parse update event, skipping");
            channel.basicAck(deliveryTag, false);
            return;
        }

        InboundMessage message = parsed.get();

        if (!userLock.tryAcquire(message.userId())) {
            log.info("User {} is locked, stashing message for merge", message.userId());
            pendingMessageBuffer.stash(message);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            processWithRateCheck(message);
            drainAndProcessPending(message.userId(), message.tenantId(), message.chatId());
        } finally {
            userLock.release(message.userId());
            MDC.remove("correlationId");
            MDC.remove("tenantId");
        }

        channel.basicAck(deliveryTag, false);
    }

    private void processWithRateCheck(InboundMessage message) {
        if (!rateLimiter.isAllowed(message.userId())) {
            replyPublisher.publish(TelegramReplyEvent.of(message.chatId(), RATE_LIMIT_REPLY));
            return;
        }

        log.info("Processing message: type={}, userId={}, chatId={}",
                message.messageType(), message.userId(), message.chatId());
        messageProcessor.process(message);
    }

    private void drainAndProcessPending(String userId, String tenantId, Long chatId) {
        List<InboundMessage> pending = pendingMessageBuffer.drainAll(userId);
        if (pending.isEmpty()) return;

        log.info("Draining {} pending messages for userId={}", pending.size(), userId);

        List<InboundMessage> textMessages = pending.stream()
                .filter(m -> m.messageType() == MessageType.TEXT && m.text() != null)
                .toList();

        List<InboundMessage> nonTextMessages = pending.stream()
                .filter(m -> m.messageType() != MessageType.TEXT)
                .toList();

        if (!textMessages.isEmpty()) {
            String merged = textMessages.stream()
                    .map(InboundMessage::text)
                    .collect(Collectors.joining("\n"));

            InboundMessage first = textMessages.getFirst();
            InboundMessage combined = new InboundMessage(
                    first.tenantId(), first.chatId(), first.userId(),
                    merged, null, MessageType.TEXT, first.customerName());

            log.info("Merged {} text messages for userId={}: '{}'", textMessages.size(), userId, merged);
            processWithRateCheck(combined);
        }

        for (InboundMessage msg : nonTextMessages) {
            processWithRateCheck(msg);
        }
    }
}
