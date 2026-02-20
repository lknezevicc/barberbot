package hr.lknezevic.barberbot.model.event;

import java.util.UUID;

public record BookingCancelRequestEvent(
        UUID bookingId,
        String userId,
        Long chatId
) {
}
