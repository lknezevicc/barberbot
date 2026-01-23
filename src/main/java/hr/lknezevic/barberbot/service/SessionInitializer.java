package hr.lknezevic.barberbot.service;

import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public final class SessionInitializer {

    private final SessionService sessionService;
    private final BotMessageService botMessageService;

    public UpdateHandlerContext create(Update update) {
        String userId = UpdateUtils.getUserId(update);

        Optional<Session> session = sessionService.getSessionByUserId(userId);

        if (session.isEmpty()) {
            Session newSession = sessionService.startSession(userId);
            botMessageService.sendWelcomeMessage(UpdateUtils.getChatId(update), newSession);
            return new UpdateHandlerContext(update, newSession);
        }

        return new UpdateHandlerContext(update, session.get());
    }

}
