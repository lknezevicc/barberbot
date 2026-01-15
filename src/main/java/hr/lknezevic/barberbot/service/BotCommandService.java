package hr.lknezevic.barberbot.service;

import hr.lknezevic.barberbot.telegram.TelegramGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.commands.GetMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public final class BotCommandService {

    private final TelegramGateway telegramGateway;

    public List<BotCommand> getCommands() throws TelegramApiException {
        log.debug("Getting commands");
        return telegramGateway.execute(new GetMyCommands());
    }

    public Boolean setCommands(Collection<? extends BotCommand> botCommands) throws TelegramApiException {
        log.debug("Setting commands");
        return telegramGateway.execute(
                SetMyCommands
                        .builder()
                        .commands(botCommands)
                        .build()
        );
    }
}
