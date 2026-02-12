package hr.lknezevic.barberbot.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum BotCommand {

    START("/start", "Pokretanje bota..."),
    MY_BOOKINGS("/my_bookings", "Moji termini"),
    CANCEL("/cancel", "Otkaži termin"),
    HELP("/help", "Pomoć");

    private final String command;
    private final String description;

    public static Optional<BotCommand> fromText(String text) {
        if (text == null) return Optional.empty();
        String trimmed = text.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(cmd -> trimmed.equals(cmd.command) || trimmed.startsWith(cmd.command + " "))
                .findFirst();
    }
}
