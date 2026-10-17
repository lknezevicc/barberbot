package hr.lknezevic.barberbot.reply.telegram;

import hr.lknezevic.barberbot.reply.config.ReplyProperties;
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
    private final ReplyProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        String token = properties.telegram().botToken();
        if (token == null || token.isBlank() || token.startsWith("your-")) {
            log.warn("Telegram bot token is not configured, skipping command sync");
            return;
        }
        try {
            botCommandService.syncCommands();
        } catch (Exception e) {
            log.warn("Failed to sync bot commands on startup: {}", e.getMessage());
        }
    }
}
