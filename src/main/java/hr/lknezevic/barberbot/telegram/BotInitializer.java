package hr.lknezevic.barberbot.telegram;

import hr.lknezevic.barberbot.enums.BarberBotCommand;
import hr.lknezevic.barberbot.service.BotCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public final class BotInitializer implements ApplicationRunner {

    private final BotCommandService botCommandService;

    @Override
    public void run(ApplicationArguments args) {
        initializeCommands();
    }

    void initializeCommands() {
        try {
            if (BarberBotCommand.differsFrom(botCommandService.getCommands())) {
                botCommandService.setCommands(BarberBotCommand.getBotCommands());
                log.debug("Commands set successfully!");
            } else {
                log.debug("Commands already up to date, no changes made.");
            }

        } catch (TelegramApiException e) {
            log.error("Failed to initialize commands", e);
        }
    }
}