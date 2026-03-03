package hr.lknezevic.barberbot.model.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record BookingSlotUnavailableEvent(
        String userId,
        Long chatId,
        String service,
        LocalDate originalDate,
        LocalTime originalTime,
        String barber,
        List<AlternativeSlot> alternatives
) {}
