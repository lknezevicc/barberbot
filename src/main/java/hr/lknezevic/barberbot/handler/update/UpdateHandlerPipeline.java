package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.HandlerResult;
import hr.lknezevic.barberbot.handler.HandlerPipeline;
import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.SessionService;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UpdateHandlerPipeline implements HandlerPipeline<Update> {

    private final SessionService sessionService;
    private final List<UpdateHandler> handlers;

    @Override
    public void process(Update value) {

        UpdateHandlerContext handlerContext =
                sessionService.getSessionByUserId(UpdateUtils.getUserId(value))
                        .map(s -> new UpdateHandlerContext(value, s))
                        .orElseGet(() -> {
                            Session session = sessionService.startSession(UpdateUtils.getUserId(value));
                            return new UpdateHandlerContext(value, session);
                        });

        for (UpdateHandler handler : handlers) {
            if (!handler.supportedStates().contains(handlerContext.sessionState())) {
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
