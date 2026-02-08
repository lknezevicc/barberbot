package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.LlmIntent;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingUpdateHandler implements UpdateHandler {

    private final BookingService bookingService;
    private final ActionTokenService actionTokenService;
    private final BotMessageService botMessageService;
    private final SessionService sessionService;
    private final UpdateService updateService;

    @Override
    public boolean supports(UpdateHandlerContext value) {
        return LlmIntent.BOOKING.equals(value.intent());
    }

    @Override
    public void handle(UpdateHandlerContext value) {
        if (value.session().bookingDraft() != null && value.session().bookingDraft().isComplete()) {
            botMessageService.sendMessage(value.chatId(), "Rezervirano: " + value.session().bookingDraft());
        }

        if (value.llmResult().bookingDraft().isComplete()) {
            botMessageService.sendMessage(value.chatId(), "Rezervirano jer LLM ima sve: "
                    + value.session().bookingDraft());
        }

        sessionService.updateSessionBookingDraft(value.session(), value.llmResult().bookingDraft());
        botMessageService.sendMessage(value.chatId(), value.llmReplyText());
    }
}
