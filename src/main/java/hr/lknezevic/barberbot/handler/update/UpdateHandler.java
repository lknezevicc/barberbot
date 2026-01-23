package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.SessionState;
import hr.lknezevic.barberbot.handler.Handler;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumSet;

public interface UpdateHandler extends Handler<UpdateHandlerContext> {
    EnumSet<SessionState> supportedStates();

    default Long getChatId(Update update) {
        return UpdateUtils.getChatId(update);
    }

    default String getUserId(Update update) {
        return UpdateUtils.getUserId(update);
    }

    default String getMessageText(Update update) {
        return UpdateUtils.getMessageText(update);
    }

    default Boolean hasMessageText(Update update) {
        return UpdateUtils.hasMessageText(update);
    }
}
