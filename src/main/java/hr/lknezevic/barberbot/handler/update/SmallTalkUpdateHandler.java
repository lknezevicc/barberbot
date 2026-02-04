package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.LlmIntent;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.BotMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmallTalkUpdateHandler implements UpdateHandler {

    private final BotMessageService botMessageService;

    @Override
    public boolean supports(UpdateHandlerContext value) {
        return LlmIntent.SMALLTALK.equals(value.intent());
    }

    @Override
    public void handle(UpdateHandlerContext value) {
        botMessageService.sendMessage(value.chatId(), value.llmReplyText());
    }
}
