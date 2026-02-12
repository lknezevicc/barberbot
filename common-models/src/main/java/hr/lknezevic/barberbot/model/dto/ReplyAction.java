package hr.lknezevic.barberbot.model.dto;

public record ReplyAction(
        Long chatId,
        String text,
        Session updatedSession
) {

    public static ReplyAction of(Long chatId, String text) {
        return new ReplyAction(chatId, text, null);
    }

    public static ReplyAction of(Long chatId, String text, Session session) {
        return new ReplyAction(chatId, text, session);
    }
}
