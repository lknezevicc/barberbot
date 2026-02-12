package hr.lknezevic.barberbot.model.dto;

import hr.lknezevic.barberbot.model.enums.BotCommand;

import java.time.LocalDateTime;

public record ActionToken(
        BotCommand action,
        String bookingId,
        LocalDateTime createdAt
) {
}
