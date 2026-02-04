package hr.lknezevic.barberbot.service;

import hr.lknezevic.barberbot.model.LlmResult;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
@RequiredArgsConstructor
public final class UpdateService {

    private final SessionService sessionService;

    public UpdateHandlerContext getContext(Update update) {
        String userId = UpdateUtils.getUserId(update);
        return sessionService.getSessionByUserId(userId)
                .map(s -> new UpdateHandlerContext(update, s, null))
                .orElseGet(() -> new UpdateHandlerContext(update, sessionService.startSession(userId), null));
    }

    public UpdateHandlerContext mergeWithLlmResult(
            UpdateHandlerContext updateHandlerContext,
            LlmResult llmResult
    ) {
        return updateHandlerContext.withSession(
                updateHandlerContext.session().withBookingDraft(
                        updateHandlerContext.session().bookingDraft().merge(llmResult.bookingDraft())
                )
        );
    }
}
