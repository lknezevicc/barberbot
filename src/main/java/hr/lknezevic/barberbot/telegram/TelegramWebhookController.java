package hr.lknezevic.barberbot.telegram;

import hr.lknezevic.barberbot.service.UpdateService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
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

    private final UpdateService updateService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdate(@RequestBody Update update) {
        try {
            log.info("onUpdate {}", update);
            updateService.enqueue(update);
            return ResponseEntity.ok().build();
        } catch (CallNotPermittedException | DataAccessException e) {
            log.warn("Enqueue failed, returning 503 to trigger Telegram retry. updateId={}, cause={}",
                    update != null ? update.getUpdateId() : null, e.toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Unexpected enqueue failure, returning 500. updateId={}",
                    update != null ? update.getUpdateId() : null, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
