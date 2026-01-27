package hr.lknezevic.barberbot.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class PromptFactory {

    public String buildPrompt() {
        return PromptConstantsUtility.BASE_INSTRUCTIONS +
                "\n" +
                PromptConstantsUtility.INTENT_BLOCK +
                "\n" +
                PromptConstantsUtility.BOOKING_DRAFT_BLOCK +
                "\n" +
                PromptConstantsUtility.REPLY_BLOCK +
                "\n" +
                PromptConstantsUtility.SECURITY_AND_FORMAT_BLOCK;
    }
}
