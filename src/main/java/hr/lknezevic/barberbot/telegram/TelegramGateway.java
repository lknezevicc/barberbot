package hr.lknezevic.barberbot.telegram;

import hr.lknezevic.barberbot.config.application.TelegramConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public final class TelegramGateway extends DefaultAbsSender {

    public TelegramGateway(TelegramConfig config, DefaultBotOptions options) {
        super(options, config.botToken());
    }

    public Message sendMessage(long chatId, String text) throws TelegramApiException {
        return execute(
                SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build()
        );
    }
}
