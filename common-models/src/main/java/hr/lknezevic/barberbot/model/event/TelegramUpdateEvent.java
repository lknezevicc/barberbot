package hr.lknezevic.barberbot.model.event;

import java.time.Instant;

public record TelegramUpdateEvent(
        String tenantId,
        String updateJson,
        String correlationId,
        Instant receivedAt
) {

    public static TelegramUpdateEvent of(String updateJson) {
        return new TelegramUpdateEvent(null, updateJson, null, Instant.now());
    }

    public static TelegramUpdateEvent of(String tenantId, String updateJson) {
        return new TelegramUpdateEvent(tenantId, updateJson, null, Instant.now());
    }

    public static TelegramUpdateEvent of(String tenantId, String updateJson, String correlationId) {
        return new TelegramUpdateEvent(tenantId, updateJson, correlationId, Instant.now());
    }
}
