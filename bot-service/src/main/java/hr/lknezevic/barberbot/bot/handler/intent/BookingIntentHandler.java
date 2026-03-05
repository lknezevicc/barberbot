package hr.lknezevic.barberbot.bot.handler.intent;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.dto.BookingDraft;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import hr.lknezevic.barberbot.model.enums.SessionState;
import hr.lknezevic.barberbot.model.event.BookingRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingIntentHandler implements IntentHandler {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public LlmIntent supportedIntent() {
        return LlmIntent.BOOKING;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session, LlmResult llmResult) {
        BookingDraft existingDraft = session.bookingDraft();
        BookingDraft incomingDraft = llmResult.bookingDraft();

        BookingDraft merged = existingDraft != null
                ? existingDraft.merge(incomingDraft)
                : (incomingDraft != null ? incomingDraft : new BookingDraft(null, null, null, null));

        Session updated = session
                .withBookingDraft(merged)
                .withState(SessionState.BOOKING);

        if (merged.isComplete()) {
            BookingRequestEvent event = new BookingRequestEvent(
                    message.userId(), message.chatId(),
                    merged.service(), merged.date(), merged.time(), merged.barber());

            rabbitTemplate.convertAndSend(
                    RabbitQueues.EXCHANGE, RabbitQueues.BOOKING_REQUEST_ROUTING_KEY, event);

            log.info("Published BookingRequestEvent (hold attempt) for userId={}, barber={}, date={}, time={}",
                    message.userId(), merged.barber(), merged.date(), merged.time());

            Session confirming = updated.withState(SessionState.CONFIRMING);
            return ReplyAction.of(message.chatId(), "Provjeravam dostupnost...", confirming);
        }

        String replyText = llmResult.reply() != null
                ? llmResult.reply()
                : "Molim te dopuni podatke za rezervaciju: " + String.join(", ", merged.missingFields());

        return ReplyAction.of(message.chatId(), replyText, updated);
    }
}
