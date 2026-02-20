package hr.lknezevic.barberbot.model.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID bookingId,
        String userId,
        Long chatId,
        String service,
        LocalDate date,
        LocalTime time,
        String barber
) {
}
