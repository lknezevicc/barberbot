package hr.lknezevic.barberbot.bot.handler.intent;

import hr.lknezevic.barberbot.bot.handler.router.CommandRouter;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandProxyIntentHandler implements IntentHandler {

    private final CommandRouter commandRouter;

    @Override
    public LlmIntent supportedIntent() {
        return LlmIntent.COMMAND_PROXY;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session, LlmResult llmResult) {
        String suggested = llmResult.suggestedCommand();

        if (suggested == null || suggested.isBlank()) {
            log.warn("COMMAND_PROXY intent but no suggestedCommand for userId={}", message.userId());
            return ReplyAction.of(message.chatId(), "Nisam siguran što trebaš. Napiši /help za popis naredbi.");
        }

        return BotCommand.fromText(suggested)
                .flatMap(commandRouter::route)
                .map(handler -> handler.handle(message, session))
                .orElseGet(() -> {
                    log.warn("LLM suggested unknown command '{}' for userId={}", suggested, message.userId());
                    return ReplyAction.of(message.chatId(), "Nisam siguran što trebaš. Napiši /help za popis naredbi.");
                });
    }
}
