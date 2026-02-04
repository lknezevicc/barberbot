package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.LlmIntent;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.BotMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnknownUpdateHandler implements UpdateHandler {

    private final BotMessageService botMessageService;

    @Override
    public boolean supports(UpdateHandlerContext value) {
        return LlmIntent.UNKNOWN.equals(value.intent());
    }

    @Override
    public void handle(UpdateHandlerContext value) {
        String reply = "Nažalost ne razumijem, možeš li pojasniti što ti treba?";
        botMessageService.sendMessage(value.chatId(), reply);
    }
}
