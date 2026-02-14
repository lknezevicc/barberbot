package hr.lknezevic.barberbot.reply.telegram;

import hr.lknezevic.barberbot.model.enums.BotCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.commands.GetMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public final class BotCommandService {

    private final TelegramGateway telegramGateway;

    public List<org.telegram.telegrambots.meta.api.objects.commands.BotCommand> getRegisteredCommands()
            throws TelegramApiException {
        return telegramGateway.execute(new GetMyCommands());
    }

    public void syncCommands() throws TelegramApiException {
        List<org.telegram.telegrambots.meta.api.objects.commands.BotCommand> desired = toBotCommands();
        List<org.telegram.telegrambots.meta.api.objects.commands.BotCommand> current = getRegisteredCommands();

        if (commandsDiffer(current, desired)) {
            telegramGateway.execute(SetMyCommands.builder().commands(desired).build());
            log.info("Telegram bot commands synced successfully");
        } else {
            log.debug("Telegram bot commands already up to date");
        }
    }

    private List<org.telegram.telegrambots.meta.api.objects.commands.BotCommand> toBotCommands() {
        return Arrays.stream(BotCommand.values())
                .map(cmd -> new org.telegram.telegrambots.meta.api.objects.commands.BotCommand(
                        cmd.getCommand().substring(1),
                        cmd.getDescription()))
                .toList();
    }

    private boolean commandsDiffer(
            Collection<? extends org.telegram.telegrambots.meta.api.objects.commands.BotCommand> current,
            Collection<? extends org.telegram.telegrambots.meta.api.objects.commands.BotCommand> desired
    ) {
        return !normalize(current).equals(normalize(desired));
    }

    private Set<String> normalize(
            Collection<? extends org.telegram.telegrambots.meta.api.objects.commands.BotCommand> commands
    ) {
        return commands.stream()
                .map(c -> c.getCommand() + "::" + c.getDescription())
                .collect(Collectors.toSet());
    }
}
