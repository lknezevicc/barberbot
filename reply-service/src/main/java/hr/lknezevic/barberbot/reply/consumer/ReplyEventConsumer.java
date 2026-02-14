package hr.lknezevic.barberbot.reply.consumer;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import hr.lknezevic.barberbot.reply.telegram.TelegramGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyEventConsumer {

    private final TelegramGateway telegramGateway;

    @RabbitListener(queues = RabbitQueues.REPLY_OUTBOUND_QUEUE)
    public void onReplyEvent(TelegramReplyEvent event) {
        log.info("Sending reply to chatId={}, tenant={}", event.chatId(), event.tenantId());

        try {
            telegramGateway.sendTextMessage(event.chatId(), event.text(), event.parseMode());
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to chatId={}", event.chatId(), e);
        }
    }
}
