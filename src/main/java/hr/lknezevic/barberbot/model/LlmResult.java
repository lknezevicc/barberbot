package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.Intent;

public record LlmResult(
        Intent intent,
        Double confidence,
        BookingDraft bookingDraft,
        String reply
) {
}
