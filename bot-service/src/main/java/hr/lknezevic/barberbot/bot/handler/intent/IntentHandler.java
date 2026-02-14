package hr.lknezevic.barberbot.bot.handler.intent;

import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.LlmIntent;

public interface IntentHandler {

    LlmIntent supportedIntent();

    ReplyAction handle(InboundMessage message, Session session, LlmResult llmResult);
}
