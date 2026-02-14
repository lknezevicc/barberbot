package hr.lknezevic.barberbot.reply.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public final class BotInitializer implements ApplicationRunner {

    private final BotCommandService botCommandService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            botCommandService.syncCommands();
        } catch (Exception e) {
            log.error("Failed to sync bot commands on startup", e);
        }
    }
}
