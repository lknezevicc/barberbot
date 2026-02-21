package hr.lknezevic.barberbot.model.dto;

import java.util.List;

public record ReplyAction(
        Long chatId,
        String text,
        Session updatedSession,
        List<InlineButton> buttons
) {

    public static ReplyAction of(Long chatId, String text) {
        return new ReplyAction(chatId, text, null, null);
    }

    public static ReplyAction of(Long chatId, String text, Session session) {
        return new ReplyAction(chatId, text, session, null);
    }

    public static ReplyAction withButtons(Long chatId, String text, Session session, List<InlineButton> buttons) {
        return new ReplyAction(chatId, text, session, buttons);
    }
}
