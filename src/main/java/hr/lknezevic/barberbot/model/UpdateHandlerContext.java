package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.BarberBotCommand;
import hr.lknezevic.barberbot.enums.SessionState;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

public record UpdateHandlerContext(
        Update update,
        Session session
) {
    public UpdateHandlerContext withSession(Session newSession) {
        return new UpdateHandlerContext(update, newSession);
    }

    public SessionState sessionState() {
        return session == null ? SessionState.NONE : session.sessionState();
    }

    public Long chatId() {
        return UpdateUtils.getChatId(update);
    }

    public String userId() {
        return UpdateUtils.getUserId(update);
    }

    public String text() {
        return UpdateUtils.getMessageText(update);
    }

    public boolean isCommand(BarberBotCommand command) {
        String text = text();
        if (text == null) return false;

        return text.trim().split(" ")[0].equalsIgnoreCase("/" + command.getCommand());
    }
}
