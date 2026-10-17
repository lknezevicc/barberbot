package hr.lknezevic.barberbot.gateway.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.gateway.filter.CorrelationIdFilter;
import hr.lknezevic.barberbot.gateway.filter.DeduplicationFilter;
import hr.lknezevic.barberbot.gateway.publisher.UpdateEventPublisher;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<Void> onUpdate(
            @PathVariable String tenantId,
            @RequestBody String rawJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            log.warn("Received malformed JSON from Telegram webhook, tenant={}", tenantId);
            return ResponseEntity.badRequest().build();
        }

        long updateId = root.path("update_id").asLong(0);
        if (updateId > 0 && deduplicationFilter.isDuplicate(updateId)) {
            log.debug("Duplicate update_id={} ignored, tenant={}", updateId, tenantId);
            return ResponseEntity.ok().build();
        }

        long chatId = root.path("message").path("chat").path("id").asLong(
                root.path("callback_query").path("message").path("chat").path("id").asLong(0));
        log.info("Received Telegram update: updateId={}, chatId={}, tenant={}", updateId, chatId, tenantId);

        try {
            String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
            TelegramUpdateEvent event = TelegramUpdateEvent.of(tenantId, rawJson, correlationId);
            updateEventPublisher.publish(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to publish update event to RabbitMQ, tenant={}", tenantId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdateLegacy(@RequestBody String rawJson) {
        return onUpdate("default", rawJson);
    }
}
