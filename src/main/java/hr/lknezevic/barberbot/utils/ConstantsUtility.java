package hr.lknezevic.barberbot.utils;

import hr.lknezevic.barberbot.enums.BarberBotCommand;
import hr.lknezevic.barberbot.model.ActionToken;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ConstantsUtility {

    public long SESSION_TTL_MINUTES = 10;

    public InlineKeyboardMarkup getMainMenu(String sessionId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (var action : BarberBotCommand.values()) {
            keyboard.add(List.of(
                    button(action, ActionToken.encode(action, sessionId))
            ));
        }

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardButton button(BarberBotCommand action, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(action.name());
        b.setCallbackData(data);
        return b;
    }
}
