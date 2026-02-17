package hr.lknezevic.barberbot.model.dto;

import hr.lknezevic.barberbot.model.enums.LlmIntent;

public record LlmResult(
        LlmIntent intent,
        BookingDraft bookingDraft,
        String suggestedCommand,
        String reply
) {
}
