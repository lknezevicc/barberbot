package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.BarberBotCommand;

import java.time.Instant;

public record ActionToken(
        BarberBotCommand action,
        String sessionId,
        long createdAt
) {
    public static String encode(BarberBotCommand command, String sessionId) {
        return command.name() + "|" + sessionId + "|" + Instant.now().getEpochSecond();
    }

    public static ActionToken decode(String data) {
        String[] dataArray = data.split("\\|");
        return new ActionToken(
                BarberBotCommand.valueOf(dataArray[0]),
                dataArray[1],
                Long.parseLong(dataArray[2])
        );
    }
}
