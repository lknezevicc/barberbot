package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.BarberBotCommand;
import hr.lknezevic.barberbot.enums.HandlerResult;
import hr.lknezevic.barberbot.enums.SessionState;
import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.BotMessageService;
import hr.lknezevic.barberbot.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements UpdateHandler {

    private final SessionService sessionService;
    private final BotMessageService botMessageService;

    @Override
    public EnumSet<SessionState> supportedStates() {
        return EnumSet.of(SessionState.NONE);
    }

    @Override
    public boolean supports(UpdateHandlerContext value) {
        return value.isCommand(BarberBotCommand.START);
    }

    @Override
    public HandlerResult handle(UpdateHandlerContext value) {
        Session session = sessionService.startSession(value.userId());
        botMessageService.sendWelcomeMessage(value.chatId(), session);

        return HandlerResult.STOP;
    }
}
