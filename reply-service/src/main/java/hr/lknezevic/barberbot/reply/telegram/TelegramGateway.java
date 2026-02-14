package hr.lknezevic.barberbot.reply.telegram;

import hr.lknezevic.barberbot.reply.config.ReplyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public final class TelegramGateway extends DefaultAbsSender {

    public TelegramGateway(ReplyProperties properties, DefaultBotOptions options) {
        super(options, properties.telegram().botToken());
    }

    public Message sendTextMessage(long chatId, String text) throws TelegramApiException {
        return sendTextMessage(chatId, text, null);
    }

    public Message sendTextMessage(long chatId, String text, String parseMode) throws TelegramApiException {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(chatId)
                .text(text);

        if (parseMode != null) {
            builder.parseMode(parseMode);
        }

        return execute(builder.build());
    }
}
