package hr.lknezevic.barberbot.bot.processor;

import hr.lknezevic.barberbot.bot.handler.router.CommandRouter;
import hr.lknezevic.barberbot.bot.handler.router.IntentRouter;
import hr.lknezevic.barberbot.bot.llm.InputSanitizer;
import hr.lknezevic.barberbot.bot.llm.LlmService;
import hr.lknezevic.barberbot.bot.llm.PromptFactory;
import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.bot.service.ConversationEntry;
import hr.lknezevic.barberbot.bot.service.ConversationHistoryService;
import hr.lknezevic.barberbot.bot.service.SessionService;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import hr.lknezevic.barberbot.model.enums.MessageType;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProcessor {

    private static final String ERROR_REPLY = "Nažalost, došlo je do greške. Pokušaj ponovo.";

    private final SessionService sessionService;
    private final ConversationHistoryService conversationHistoryService;
    private final InputSanitizer inputSanitizer;
    private final LlmService llmService;
    private final CommandRouter commandRouter;
    private final IntentRouter intentRouter;
    private final ReplyPublisher replyPublisher;

    public void process(InboundMessage message) {
        try {
            Session session = sessionService.getOrCreateSession(message.userId());

            ReplyAction action = switch (message.messageType()) {
                case COMMAND -> handleCommand(message, session);
                case TEXT -> handleText(message, session);
                case CALLBACK -> handleCallback(message, session);
            };

            if (action == null) {
                log.warn("No reply action for message userId={}, type={}", message.userId(), message.messageType());
                return;
            }

            if (action.updatedSession() != null) {
                sessionService.saveSession(action.updatedSession());
            } else {
                sessionService.refreshSession(session.id());
            }

            if (message.messageType() == MessageType.TEXT && message.text() != null) {
                conversationHistoryService.append(message.userId(), ConversationEntry.user(message.text()));
                conversationHistoryService.append(message.userId(), ConversationEntry.assistant(action.text()));
            }

            publishReply(message.tenantId(), action);

        } catch (Exception e) {
            log.error("Failed to process message for userId={}", message.userId(), e);
            publishReply(message.tenantId(), ReplyAction.of(message.chatId(), ERROR_REPLY));
        }
    }

    private ReplyAction handleCommand(InboundMessage message, Session session) {
        return BotCommand.fromText(message.text())
                .flatMap(commandRouter::route)
                .map(handler -> handler.handle(message, session))
                .orElseGet(() -> ReplyAction.of(
                        message.chatId(),
                        "Nepoznata naredba. Napiši /help za popis naredbi."
                ));
    }

    private ReplyAction handleText(InboundMessage message, Session session) {
        InputSanitizer.SanitizationResult sanitized = inputSanitizer.sanitize(message.text());

        if (sanitized.injectionSuspected()) {
            log.warn("Prompt injection suspected for userId={}, flagging to LLM", message.userId());
        }

        List<ConversationEntry> history = conversationHistoryService.getHistory(message.userId());

        LlmResult llmResult = llmService.ask(
                PromptFactory.buildSystemPrompt(),
                sanitized.text(),
                history,
                LlmResult.class
        );

        log.info("LLM classified intent={} for userId={}", llmResult.intent(), message.userId());

        return intentRouter.route(llmResult.intent())
                .map(handler -> handler.handle(message, session, llmResult))
                .orElseGet(() -> {
                    log.warn("No intent handler for intent={}", llmResult.intent());
                    String reply = llmResult.reply() != null
                            ? llmResult.reply()
                            : "Nisam siguran što trebaš. Pokušaj ponovo ili napiši /help.";
                    return ReplyAction.of(message.chatId(), reply);
                });
    }

    private ReplyAction handleCallback(InboundMessage message, Session session) {
        log.info("Callback received from userId={}, data={}", message.userId(), message.callbackData());
        return ReplyAction.of(message.chatId(), "Ova akcija još nije podržana.");
    }

    private void publishReply(String tenantId, ReplyAction action) {
        TelegramReplyEvent event = TelegramReplyEvent.of(tenantId, action.chatId(), action.text());
        replyPublisher.publish(event);
    }
}
