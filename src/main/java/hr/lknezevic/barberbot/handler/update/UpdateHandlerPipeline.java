package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.handler.HandlerPipeline;
import hr.lknezevic.barberbot.model.LlmResult;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.LlmService;
import hr.lknezevic.barberbot.service.SessionService;
import hr.lknezevic.barberbot.service.UpdateService;
import hr.lknezevic.barberbot.utils.PromptFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UpdateHandlerPipeline implements HandlerPipeline<Update> {

    private final SessionService sessionService;
    private final LlmService llmService;
    private final UpdateService updateService;
    private final List<UpdateHandler> handlers;

    @Override
    public void process(Update value) {
        try {
            if (value.hasCallbackQuery()) {
                handleCallback(value);
                return;
            }

            handleMessage(value);
        } catch (Exception e) {
            log.error("Error processing updateId={}", value.getUpdateId(), e);
        }
    }

    private void handleCallback(Update update) {
        UpdateHandlerContext ctx = updateService.getContext(update);
        dispatch(ctx);
    }

    private void handleMessage(Update update) {
        UpdateHandlerContext ctx = updateService.getContext(update);

        LlmResult llmResult = llmService.ask(
                PromptFactory.buildPrompt(),
                ctx.messageText(),
                LlmResult.class
        );

        log.info("LLM response: {}", llmResult);

        dispatch(updateService.mergeWithLlmResult(ctx, llmResult));
    }

    private void dispatch(UpdateHandlerContext ctx) {
        handlers.stream()
                .filter(handler -> handler.supports(ctx))
                .findFirst()
                .ifPresentOrElse(
                        handler -> {
                            handler.handle(ctx);
                            sessionService.refreshSession(ctx.session().id());
                        },
                        () -> log.warn("No handler for update={}, user={}",
                                ctx.update(),
                                ctx.userId())
                );
    }
}
