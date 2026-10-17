package hr.lknezevic.barberbot.reply.consumer;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import hr.lknezevic.barberbot.reply.telegram.TenantBotRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyEventConsumer {

    private final TenantBotRegistry tenantBotRegistry;

    @RabbitListener(queues = RabbitQueues.REPLY_OUTBOUND_QUEUE)
    public void onReplyEvent(TelegramReplyEvent event) {
        log.info("Sending reply to chatId={}, tenant={}, hasButtons={}, hasDocument={}",
                event.chatId(), event.tenantId(),
                event.inlineKeyboard() != null && !event.inlineKeyboard().isEmpty(),
                event.documentData() != null);

        try {
            if (event.documentData() != null && event.documentData().length > 0) {
                tenantBotRegistry.sendDocument(
                        event.tenantId(), event.chatId(), event.text(),
                        event.documentData(), event.documentFilename());
            } else {
                tenantBotRegistry.sendTextMessage(
                        event.tenantId(), event.chatId(), event.text(),
                        event.parseMode(), event.inlineKeyboard());
            }
            log.info("Reply sent successfully to chatId={}, tenant={}", event.chatId(), event.tenantId());
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to chatId={}, tenant={}",
                    event.chatId(), event.tenantId(), e);
        }
    }
}
