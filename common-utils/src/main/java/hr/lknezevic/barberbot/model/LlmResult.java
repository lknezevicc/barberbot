package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.LlmIntent;

public record LlmResult(
        LlmIntent intent,
        Double confidence,
        BookingDraft bookingDraft,
        String reply
) {
}
