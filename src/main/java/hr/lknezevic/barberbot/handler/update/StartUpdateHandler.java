package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.LlmIntent;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.BotMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartUpdateHandler implements UpdateHandler {

    private final BotMessageService botMessageService;

    @Override
    public boolean supports(UpdateHandlerContext value) {
        return LlmIntent.START.equals(value.intent()) || value.messageText().contains("/start");
    }

    @Override
    public void handle(UpdateHandlerContext value) {
        String reply = "Pozdrav, ja sam BarberBot! ✂️ Kako ti mogu pomoći?";
        botMessageService.sendMessage(value.chatId(), reply);
    }
}
