package hr.lknezevic.barberbot.bot.handler.intent;

import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import org.springframework.stereotype.Component;

@Component
public class SmallTalkIntentHandler implements IntentHandler {

    @Override
    public LlmIntent supportedIntent() {
        return LlmIntent.SMALLTALK;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session, LlmResult llmResult) {
        String reply = llmResult.reply() != null
                ? llmResult.reply()
                : "Hej! Ako trebaš pomoć, napiši /help.";
        return ReplyAction.of(message.chatId(), reply);
    }
}
