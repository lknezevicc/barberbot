package hr.lknezevic.barberbot.webhook.api;

import hr.lknezevic.barberbot.webhook.service.WebhookIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public final class TelegramWebhookController {

    private final WebhookIngestionService webhookIngestionService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdate(@RequestBody Update update) {
        // TODO
        return ResponseEntity.ok().build();
    }
}
