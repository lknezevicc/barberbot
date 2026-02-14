package hr.lknezevic.barberbot.bot.handler.intent;

import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import org.springframework.stereotype.Component;

@Component
public class UnknownIntentHandler implements IntentHandler {

    @Override
    public LlmIntent supportedIntent() {
        return LlmIntent.UNKNOWN;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session, LlmResult llmResult) {
        String reply = llmResult.reply() != null
                ? llmResult.reply()
                : "Nažalost ne razumijem, možeš li pojasniti što ti treba?";
        return ReplyAction.of(message.chatId(), reply);
    }
}
