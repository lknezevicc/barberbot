package hr.lknezevic.barberbot.model.event;

import java.util.UUID;

public record BookingConfirmHoldEvent(
        UUID bookingId,
        String userId,
        Long chatId,
        String tenantId
) {}
