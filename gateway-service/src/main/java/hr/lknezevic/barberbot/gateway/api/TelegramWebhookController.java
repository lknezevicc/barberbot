package hr.lknezevic.barberbot.gateway.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.gateway.filter.DeduplicationFilter;
import hr.lknezevic.barberbot.gateway.publisher.UpdateEventPublisher;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public final class TelegramWebhookController {

    private final UpdateEventPublisher updateEventPublisher;
    private final DeduplicationFilter deduplicationFilter;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdate(@RequestBody String rawJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            log.warn("Received malformed JSON from Telegram webhook");
            return ResponseEntity.badRequest().build();
        }

        long updateId = root.path("update_id").asLong(0);
        if (updateId > 0 && deduplicationFilter.isDuplicate(updateId)) {
            return ResponseEntity.ok().build();
        }

        try {
            TelegramUpdateEvent event = TelegramUpdateEvent.of(rawJson);
            updateEventPublisher.publish(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to publish update event to RabbitMQ", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
