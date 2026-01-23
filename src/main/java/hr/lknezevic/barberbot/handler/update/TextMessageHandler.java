package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TextMessageHandler implements UpdateHandler {

    @Override
    public boolean supports(Update value) {
        return hasMessageText(value);
    }

    @Override
    public void handle(Update value, UpdateHandlerContext context) {

    }
}
