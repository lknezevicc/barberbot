package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.HandlerResult;
import hr.lknezevic.barberbot.handler.HandlerPipeline;
import hr.lknezevic.barberbot.model.ActionToken;
import hr.lknezevic.barberbot.model.LlmResult;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.BotMessageService;
import hr.lknezevic.barberbot.service.LlmService;
import hr.lknezevic.barberbot.service.SessionService;
import hr.lknezevic.barberbot.utils.PromptFactory;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UpdateHandlerPipeline implements HandlerPipeline<Update> {

    private final SessionService sessionService;
    private final LlmService llmService;
    private final BotMessageService botMessageService;
    private final List<UpdateHandler> handlers;

    @Override
    public void process(Update value) {

        if (value.hasCallbackQuery()) {
            String data = value.getCallbackQuery().getData();
            ActionToken token = ActionToken.decode(data);
            switch (token.action()) {
                case CANCEL -> bookingService.cancelAppointement(token.);
            }
        }




        String userId = UpdateUtils.getUserId(value);
        UpdateHandlerContext handlerContext = sessionService.getSessionByUserId(userId)
                .map(s -> new UpdateHandlerContext(value, s))
                .orElseGet(() -> new UpdateHandlerContext(value, sessionService.startSession(userId)));






        LlmResult llmResult = llmService.ask(
                PromptFactory.buildPrompt(),
                handlerContext.text(),
                LlmResult.class
        );

        UpdateHandlerContext newCtx = handlerContext.withSession(
                handlerContext.session().withBookingDraft(
                        handlerContext.session().bookingDraft().merge(llmResult.bookingDraft())
                )
        );

        log.info("LLM response: {}", llmResult);

        botMessageService.sendMessage(handlerContext.chatId(), llmResult.reply());

        for (UpdateHandler handler : handlers) {
            if (!handler.) {
                continue;
            }

            if (handler.supports(handlerContext)) {
                HandlerResult result = handler.handle(handlerContext);

                sessionService.refreshSessionByUserId(handlerContext.userId());

                if (result == HandlerResult.STOP) return;
            }
        }

        log.warn("No handler found for updateId={}, state={}", value.getUpdateId(), handlerContext.sessionState());
    }
}



START,
BOOKING,
MY_BOOKINGS,
CANCEL,
HELP,
SMALLTALK,
UNKNOWN
