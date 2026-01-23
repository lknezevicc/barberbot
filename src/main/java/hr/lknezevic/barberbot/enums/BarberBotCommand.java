package hr.lknezevic.barberbot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum BarberBotCommand {
    START("start", "Pokretanje bota..."),
    BOOK("rezerviraj", "📅 Rezerviraj termin"),
    MY_BOOKINGS("my_bookings", "📖 Moji termini"),
    CANCEL("cancel", "Otkaži termin"),
    HELP("help", "ℹ️ Pomoć");

    final String command;
    final String description;

    public static List<BotCommand> getBotCommands() {
        return Arrays.stream(values())
                .map(c -> new BotCommand(c.getCommand(), c.getDescription()))
                .toList();
    }

    public static boolean differsFrom(Collection<? extends BotCommand> current) {
        return !normalize(current).equals(normalize(getBotCommands()));
    }

    static Set<String> normalize(Collection<? extends BotCommand> botCommands) {
        return botCommands.stream()
                .map(c -> c.getCommand() + "::" + c.getDescription())
                .collect(Collectors.toSet());
    }
}
