package hr.lknezevic.barberbot.bot.processor;

import hr.lknezevic.barberbot.bot.handler.router.CommandRouter;
import hr.lknezevic.barberbot.bot.handler.router.IntentRouter;
import hr.lknezevic.barberbot.bot.handler.intent.IntentHandler;
import hr.lknezevic.barberbot.bot.handler.command.CommandHandler;
import hr.lknezevic.barberbot.bot.llm.InputSanitizer;
import hr.lknezevic.barberbot.bot.llm.LlmService;
import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.bot.service.ConversationEntry;
import hr.lknezevic.barberbot.bot.service.ConversationHistoryService;
import hr.lknezevic.barberbot.bot.service.SessionService;
import hr.lknezevic.barberbot.bot.service.TenantContextService;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import hr.lknezevic.barberbot.model.enums.MessageType;
import hr.lknezevic.barberbot.model.enums.SessionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Mock private SessionService sessionService;
    @Mock private ConversationHistoryService conversationHistoryService;
    @Mock private InputSanitizer inputSanitizer;
    @Mock private LlmService llmService;
    @Mock private CommandRouter commandRouter;
    @Mock private IntentRouter intentRouter;
    @Mock private ReplyPublisher replyPublisher;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private TenantContextService tenantContextService;

    @InjectMocks
    private MessageProcessor messageProcessor;

    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session("sess-1", "user-42", SessionState.IDLE, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void commandMessage_routedToCommandHandler() {
        InboundMessage msg = new InboundMessage("t1", 123L, "user-42", "/help", null, MessageType.COMMAND, "Leon");
        when(sessionService.getOrCreateSession("user-42")).thenReturn(session);

        CommandHandler handler = mock(CommandHandler.class);
        when(handler.handle(msg, session)).thenReturn(ReplyAction.of(123L, "Pomoć..."));
        when(commandRouter.route(any())).thenReturn(Optional.of(handler));

        messageProcessor.process(msg);

        verify(replyPublisher).publish(any());
    }

    @Test
    void textMessage_routedThroughLlm() {
        InboundMessage msg = new InboundMessage("t1", 123L, "user-42", "bok", null, MessageType.TEXT, "Leon");
        when(sessionService.getOrCreateSession("user-42")).thenReturn(session);
        when(inputSanitizer.sanitize("bok")).thenReturn(new InputSanitizer.SanitizationResult("bok", false));
        when(conversationHistoryService.getHistory("user-42")).thenReturn(List.of());

        TenantContextService.TenantContext ctx = new TenantContextService.TenantContext(List.of(), List.of(), List.of());
        when(tenantContextService.getContext("t1")).thenReturn(ctx);

        LlmResult llmResult = new LlmResult(LlmIntent.SMALLTALK, null, null, "Bok!");
        when(llmService.ask(anyString(), eq("bok"), eq(List.of()), eq(LlmResult.class))).thenReturn(llmResult);

        IntentHandler intentHandler = mock(IntentHandler.class);
        when(intentHandler.handle(msg, session, llmResult)).thenReturn(ReplyAction.of(123L, "Bok!"));
        when(intentRouter.route(LlmIntent.SMALLTALK)).thenReturn(Optional.of(intentHandler));

        messageProcessor.process(msg);

        verify(replyPublisher).publish(any());
        verify(conversationHistoryService, atLeast(1)).append(eq("user-42"), any(ConversationEntry.class));
    }

    @Test
    void exceptionDuringProcessing_publishesErrorReply() {
        InboundMessage msg = new InboundMessage("t1", 123L, "user-42", "bok", null, MessageType.TEXT, "Leon");
        when(sessionService.getOrCreateSession("user-42")).thenThrow(new RuntimeException("Redis down"));

        messageProcessor.process(msg);

        verify(replyPublisher).publish(any());
    }

    @Test
    void sessionSaved_afterSuccessfulAction() {
        InboundMessage msg = new InboundMessage("t1", 123L, "user-42", "/help", null, MessageType.COMMAND, "Leon");
        when(sessionService.getOrCreateSession("user-42")).thenReturn(session);

        CommandHandler handler = mock(CommandHandler.class);
        when(handler.handle(msg, session)).thenReturn(ReplyAction.of(123L, "Help text"));
        when(commandRouter.route(any())).thenReturn(Optional.of(handler));

        messageProcessor.process(msg);

        verify(sessionService).refreshSession("sess-1");
    }

    @Test
    void callbackMessage_handledCorrectly() {
        InboundMessage msg = new InboundMessage("t1", 123L, "user-42", null, "booking:cancel", MessageType.CALLBACK, "Leon");
        Session confirmingSession = session.withState(SessionState.CONFIRMING);
        when(sessionService.getOrCreateSession("user-42")).thenReturn(confirmingSession);

        messageProcessor.process(msg);

        verify(replyPublisher).publish(any());
    }
}
