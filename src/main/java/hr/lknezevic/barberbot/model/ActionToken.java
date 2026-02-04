package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.BarberBotCommand;

import java.time.LocalDateTime;

public record ActionToken(
        BarberBotCommand action,
        String bookingId,
        LocalDateTime createdAt
) { }
