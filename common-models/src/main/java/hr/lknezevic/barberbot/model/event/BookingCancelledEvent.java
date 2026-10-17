package hr.lknezevic.barberbot.model.event;

import java.util.UUID;

public record BookingCancelledEvent(
        UUID bookingId,
        String userId,
        Long chatId,
        String tenantId,
        boolean success,
        String message
) {}
