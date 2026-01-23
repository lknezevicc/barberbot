package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.handler.HandlerPipeline;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.SessionInitializer;
import hr.lknezevic.barberbot.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UpdateHandlerPipeline implements HandlerPipeline<Update> {

    private final SessionInitializer sessionInitializer;
    private final SessionService sessionService;
    private final List<UpdateHandler> handlers;

    @Override
    public void process(Update value) {
        UpdateHandlerContext handlerContext = sessionInitializer.create(value);

        for (UpdateHandler handler : handlers) {
            if (!handler.supportedStates().contains(handlerContext.session().sessionState())) {
                continue;
            }

            if (handler.supports(handlerContext)) {
                handler.handle(handlerContext);

                sessionService.refreshSession(handlerContext.session());
                return;
            }
        }

        log.warn("No handler found for updateId={}, state={}",
                value.getUpdateId(),
                handlerContext.session().sessionState());
    }
}
