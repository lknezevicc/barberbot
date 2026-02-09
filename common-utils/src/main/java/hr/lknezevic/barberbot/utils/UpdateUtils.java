package hr.lknezevic.barberbot.utils;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Update;

@UtilityClass
public class UpdateUtils {

    public String getUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId().toString();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId().toString();
        } else if (update.hasInlineQuery()) {
            return update.getInlineQuery().getFrom().getId().toString();
        }

        return null;
    }

    public Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        } else if (update.hasInlineQuery()) {
            return update.getInlineQuery().getFrom().getId();
        }

        return null;
    }

    public String getMessageText(Update update) {
        return hasMessageText(update) ?
                update.getMessage().getText() :
                null;
    }

    public Boolean hasMessageText(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }

}
