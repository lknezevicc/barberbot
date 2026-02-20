package hr.lknezevic.barberbot.model.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequestEvent(
        String userId,
        Long chatId,
        String service,
        LocalDate date,
        LocalTime time,
        String barber
) {
}
