package hr.lknezevic.barberbot.bot.handler.intent;

import hr.lknezevic.barberbot.model.dto.BookingDraft;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.InlineButton;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import hr.lknezevic.barberbot.model.enums.SessionState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingIntentHandler implements IntentHandler {

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
            String summary = merged.service() + " kod " + merged.barber()
                    + " na " + merged.date() + " u " + merged.time();

            List<InlineButton> buttons = List.of(
                    new InlineButton("Potvrdi", "booking:confirm"),
                    new InlineButton("Odustani", "booking:cancel")
            );

            return ReplyAction.withButtons(
                    message.chatId(),
                    summary + "\n\nPotvrđuješ?",
                    updated.withState(SessionState.CONFIRMING),
                    buttons
            );
        }

        String replyText = llmResult.reply() != null
                ? llmResult.reply()
                : "Molim te dopuni podatke za rezervaciju: " + String.join(", ", merged.missingFields());

        return ReplyAction.of(message.chatId(), replyText, updated);
    }
}
