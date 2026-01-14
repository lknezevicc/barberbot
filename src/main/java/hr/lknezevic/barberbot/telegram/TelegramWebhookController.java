package hr.lknezevic.barberbot.telegram;

import hr.lknezevic.barberbot.config.ApplicationConfig;
import hr.lknezevic.barberbot.config.TelegramConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public final class TelegramWebhookController {
    private final TelegramConfig telegramConfig;
    private final ApplicationConfig applicationConfig;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdate(
            @RequestHeader(
                    value = "X-Telegram-Bot-Api-Secret-Token",
                    required = false
            ) String secretHeader,
            @RequestBody String body
    ) {
        if (!applicationConfig.enabled()) {
            log.warn("Telegram Webhook API is disabled");
            return ResponseEntity.ok().build();
        }

        if (!telegramConfig.secretToken().equals(secretHeader)) {
            log.warn("Invalid Telegram secret token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Update update = objectMapper.readValue(body, Update.class);
            log.info("Received message: {}", update.getMessage());
        } catch (JacksonException e) {
            log.error("Failed to parse update", e);
        }

        return ResponseEntity.ok().build();
    }
}
