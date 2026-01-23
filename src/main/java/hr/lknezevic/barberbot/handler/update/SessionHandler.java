package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.SessionState;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.service.BotMessageService;
import hr.lknezevic.barberbot.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumSet;
import java.util.Optional;

@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class SessionHandler implements UpdateHandler {

    private final SessionService sessionService;
    private final BotMessageService botMessageService;

    @Override
    public EnumSet<SessionState> supportedStates() {
        return EnumSet.of(SessionState.IDLE)
    }

    @Override
    public boolean supports(Update value) {
        return hasMessageText(value);
    }

    @Override
    public void handle(Update value, UpdateHandlerContext context) {
        Optional<Session> session = sessionService.getSessionByUserId(getUserId(value));

        if (session.isEmpty()) {
            botMessageService.sendMessage(getChatId(value), "Session not found");
        }
    }
}
