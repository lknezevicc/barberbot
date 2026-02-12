package hr.lknezevic.barberbot.model.event;

public record TelegramReplyEvent(
        String tenantId,
        Long chatId,
        String text,
        String parseMode
) {

    public static TelegramReplyEvent of(Long chatId, String text) {
        return new TelegramReplyEvent(null, chatId, text, null);
    }

    public static TelegramReplyEvent of(String tenantId, Long chatId, String text) {
        return new TelegramReplyEvent(tenantId, chatId, text, null);
    }
}
