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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingIntentHandler implements IntentHandler {

    private static final int MAX_ADVANCE_DAYS = 30;
    private static final Set<String> KNOWN_SERVICES = Set.of(
            "šišanje", "sisanje", "brijanje", "šišanje i brijanje", "sisanje i brijanje");

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

        if (!merged.isComplete()) {
            String replyText = llmResult.reply() != null
                    ? llmResult.reply()
                    : "Molim te dopuni podatke za rezervaciju: " + String.join(", ", merged.missingFields());
            return ReplyAction.of(message.chatId(), replyText, updated);
        }

        String validationError = validate(merged);
        if (validationError != null) {
            BookingDraft cleared = clearInvalidFields(merged);
            Session correcting = updated.withBookingDraft(cleared).withState(SessionState.BOOKING);
            return ReplyAction.of(message.chatId(), validationError, correcting);
        }

        BookingDraft normalized = normalize(merged);
        updated = updated.withBookingDraft(normalized);

        BookingRequestEvent event = new BookingRequestEvent(
                message.userId(), message.chatId(),
                normalized.service(), normalized.date(), normalized.time(), normalized.barber());

        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE, RabbitQueues.BOOKING_REQUEST_ROUTING_KEY, event);

        log.info("Published BookingRequestEvent (hold attempt) for userId={}, barber={}, date={}, time={}",
                message.userId(), normalized.barber(), normalized.date(), normalized.time());

        Session confirming = updated.withState(SessionState.CONFIRMING);
        return ReplyAction.of(message.chatId(), "Provjeravam dostupnost...", confirming);
    }

    private String validate(BookingDraft draft) {
        LocalDate today = LocalDate.now();

        if (draft.date().isBefore(today)) {
            return "Taj datum je prošao. Koji drugi datum ti odgovara?";
        }

        if (draft.date().isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            return "Mogu rezervirati maksimalno " + MAX_ADVANCE_DAYS + " dana unaprijed. Odaberi bliži datum.";
        }

        return null;
    }

    private BookingDraft clearInvalidFields(BookingDraft draft) {
        LocalDate today = LocalDate.now();
        LocalDate date = draft.date();

        if (date != null && (date.isBefore(today) || date.isAfter(today.plusDays(MAX_ADVANCE_DAYS)))) {
            date = null;
        }

        return new BookingDraft(draft.service(), date, draft.time(), draft.barber());
    }

    private BookingDraft normalize(BookingDraft draft) {
        LocalTime rounded = roundToGrid(draft.time());
        String normalizedService = normalizeService(draft.service());
        String normalizedBarber = draft.barber().trim();

        return new BookingDraft(normalizedService, draft.date(), rounded, normalizedBarber);
    }

    private LocalTime roundToGrid(LocalTime time) {
        int minutes = time.getMinute();
        int remainder = minutes % 5;
        if (remainder == 0) return time.withSecond(0).withNano(0);

        if (remainder < 3) {
            return time.minusMinutes(remainder).withSecond(0).withNano(0);
        }
        return time.plusMinutes(5 - remainder).withSecond(0).withNano(0);
    }

    private String normalizeService(String service) {
        String trimmed = service.trim().toLowerCase();
        if (KNOWN_SERVICES.contains(trimmed)) {
            return trimmed;
        }
        if (trimmed.contains("šišanj") || trimmed.contains("sisanj") || trimmed.contains("haircut")) {
            return "šišanje";
        }
        if (trimmed.contains("brij") || trimmed.contains("shave")) {
            return "brijanje";
        }
        return trimmed;
    }
}
