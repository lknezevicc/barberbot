package hr.lknezevic.barberbot.model.event;

import hr.lknezevic.barberbot.model.dto.InlineButton;

import java.util.List;

public record TelegramReplyEvent(
        String tenantId,
        Long chatId,
        String text,
        String parseMode,
        List<InlineButton> inlineKeyboard,
        byte[] documentData,
        String documentFilename
) {

    public static TelegramReplyEvent of(Long chatId, String text) {
        return new TelegramReplyEvent(null, chatId, text, null, null, null, null);
    }

    public static TelegramReplyEvent of(String tenantId, Long chatId, String text) {
        return new TelegramReplyEvent(tenantId, chatId, text, null, null, null, null);
    }

    public static TelegramReplyEvent withButtons(String tenantId, Long chatId, String text, List<InlineButton> buttons) {
        return new TelegramReplyEvent(tenantId, chatId, text, null, buttons, null, null);
    }

    public static TelegramReplyEvent withDocument(String tenantId, Long chatId, String text,
                                                   byte[] documentData, String filename) {
        return new TelegramReplyEvent(tenantId, chatId, text, null, null, documentData, filename);
    }
}
