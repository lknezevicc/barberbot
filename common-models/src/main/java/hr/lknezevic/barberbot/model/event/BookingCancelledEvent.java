package hr.lknezevic.barberbot.model.event;

import java.util.UUID;

public record BookingCancelledEvent(
        UUID bookingId,
        String userId,
        Long chatId,
        boolean success,
        String message
) {
}
