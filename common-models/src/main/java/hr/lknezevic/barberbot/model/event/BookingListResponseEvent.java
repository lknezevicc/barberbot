package hr.lknezevic.barberbot.model.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record BookingListResponseEvent(
        String userId,
        Long chatId,
        List<BookingSummary> bookings
) {

    public record BookingSummary(
            UUID id,
            String service,
            LocalDate date,
            LocalTime time,
            String barber,
            String status
    ) {
    }
}
