package hr.lknezevic.barberbot.model;

import org.telegram.telegrambots.meta.api.objects.Update;

public record UpdateHandlerContext(
        Update update,
        Session session
) {
    public UpdateHandlerContext withSession(Session newSession) {
        return new UpdateHandlerContext(update, newSession);
    }
}
