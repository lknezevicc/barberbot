package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.LlmIntent;
import hr.lknezevic.barberbot.utils.UpdateUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

public record UpdateHandlerContext(
        Update update,
        Session session,
        LlmResult llmResult
) {
    public boolean hasLlmResult() {
        return llmResult != null;
    }

    public LlmIntent intent() {
        return llmResult != null ? llmResult.intent() : LlmIntent.UNKNOWN;
    }

    public UpdateHandlerContext withSession(Session newSession) {
        return new UpdateHandlerContext(update, newSession, llmResult);
    }

    public UpdateHandlerContext withLlmResult(LlmResult result) {
        return new UpdateHandlerContext(update, session, result);
    }

    public Long chatId() {
        return UpdateUtils.getChatId(update);
    }

    public String userId() {
        return UpdateUtils.getUserId(update);
    }

    public String messageText() {
        return UpdateUtils.getMessageText(update);
    }

    public String llmReplyText() {
        return llmResult != null ? llmResult.reply() : "";
    }
}
