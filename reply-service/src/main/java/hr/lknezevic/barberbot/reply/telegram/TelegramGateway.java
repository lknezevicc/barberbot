package hr.lknezevic.barberbot.reply.telegram;

import hr.lknezevic.barberbot.model.dto.InlineButton;
import hr.lknezevic.barberbot.reply.config.ReplyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Component
public final class TelegramGateway extends DefaultAbsSender {

    public TelegramGateway(ReplyProperties properties, DefaultBotOptions options) {
        super(options, properties.telegram().botToken());
    }

    public Message sendTextMessage(long chatId, String text) throws TelegramApiException {
        return sendTextMessage(chatId, text, null, null);
    }

    public Message sendTextMessage(long chatId, String text, String parseMode) throws TelegramApiException {
        return sendTextMessage(chatId, text, parseMode, null);
    }

    public Message sendTextMessage(long chatId, String text, String parseMode,
                                   List<InlineButton> inlineButtons) throws TelegramApiException {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(chatId)
                .text(text);

        if (parseMode != null) {
            builder.parseMode(parseMode);
        }

        if (inlineButtons != null && !inlineButtons.isEmpty()) {
            List<InlineKeyboardButton> row = inlineButtons.stream()
                    .map(btn -> {
                        InlineKeyboardButton kb = new InlineKeyboardButton();
                        kb.setText(btn.text());
                        kb.setCallbackData(btn.callbackData());
                        return kb;
                    })
                    .toList();

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(List.of(row));
            builder.replyMarkup(markup);
        }

        return execute(builder.build());
    }
}
